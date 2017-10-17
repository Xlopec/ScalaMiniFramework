package core.di.imp

import java.io.File
import java.lang.annotation.Annotation
import java.lang.reflect.{Constructor, Method, Modifier}
import java.net.URL
import java.util

import core.di.annotation.{Singleton => _, _}
import core.di.settings._
import core.di.{ConfigParser, settings}
import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.language.postfixOps
import scala.xml.{Elem, MetaData, Node, XML}

/**
  * Created by Максим on 9/19/2017.
  */
final class XmlParser(file: File) extends ConfigParser {
  require(file != null, "file == null")
  require(file.exists() && file.isFile && file.canRead,
    s"""Couldn't read xml config file ${file.getAbsolutePath}:
       |exists -${file.exists()},
       |is file -${file.isFile},
       |readable -${file.canRead}
   """.stripMargin)

  override def parse(): ContextSettings = {
    val xml = XML.loadFile(file)
    val context = new Context(xml \ "property" map parseProperties toMap)
    val declaredBeans = xml \ "bean" map (node => parseBeanDeclaration(node, context)) toList
    val scanSettings = xml \ "component-scan" map parseScanSetting toList

    ContextSettings(declaredBeans ++ resolveScannedBeans(scanSettings, context))
  }

  private def parseProperties(node: Node) = {
    val id = node \ "@id" text
    val attributes = node.attributes
    val valueOpt = attributes.get("value")

    require(valueOpt.nonEmpty,
      s"""Not found attribute, named 'value'
         |in node $node""".stripMargin)

    val typeOpt = attributes.get("type")

    require(typeOpt.nonEmpty,
      s"""Not found attribute, named 'type'
         |in node $node""".stripMargin)

    val pair = typeMapping.getOrElse(typeOpt.get.text, throw new RuntimeException(s"Not found type for ${typeOpt.get.text}"))

    (id, Property(pair.getWrappedClass, pair.transform(valueOpt.get.text)))
  }

  private def parseBeanDeclaration(node: Node, context: Context) = {
    val id = node \ "@id" text
    val classOf = Class.forName(node \ "@class" text).asInstanceOf[Class[AnyRef]]

    BeanDeclaration(id, classOf, parseScope(node), parseDependencies(node, classOf, context))
  }

  private def parseScanSetting(node: Node) = ScanSettings(node \ "@base-package" text)

  private def parseDependencies(node: Node, classOf: Class[_], context: Context): List[Dependency] = {
    val constructorDeps = node \ "constructor-arg" map (node => parseConstructorDeps(node.asInstanceOf[Elem], context)) toList
    val methods = classOf.getMethods.filter(m => isInjectable(m))
    val setterDeps = node \ "property" map (node => parseSetterDeps(node.asInstanceOf[Elem], methods, context)) toList

    constructorDeps ++ setterDeps
  }

  private def parseDependencies(classOf: Class[_ <: AnyRef], context: Context): List[Dependency] =
    parseConstructorDeps(classOf, context) ++ parseSetterDeps(classOf, context)

  private def parseConstructorDeps(node: Elem, context: Context) = {
    val attributes = node.attributes
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 1,
        s"""Invalid attributes length, should be 1 but were ${attributes.length}
           |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), settings.Constructor)
    } else {
      parsePrimitive(node, context, settings.Constructor)
    }
  }

  private def isInjectable(method: Method) =
    method.getParameterCount == 1 && (method.getModifiers & Modifier.PUBLIC) != 0 && method.getReturnType.getName.equals("void")

  private def parseSetterDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val dependencies = for (method <- cl.getMethods if method.isAnnotationPresent(classOf[Autowiring])) yield {
      require(isInjectable(method), s"Method $method isn't injectable")

      val cl = method.getParameterTypes()(0)
      val autowiring = method.getAnnotation[Autowiring](classOf[Autowiring ])
      val id = extractId(cl, autowiring)
      val depType = typeMapping.get(cl.getSimpleName)

      if (depType.isEmpty) {
        Dependency(Right(BeanRef(id)), Setter(method.getName))
      } else {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.properties(id)), Setter(method.getName))
      }
    }

    dependencies toList
  }

  private def parseSetterDeps(node: Elem, methods: Array[Method], context: Context): Dependency = {
    val attributes = node.attributes
    val nameOpt = attributes.get("name")
    require(nameOpt.isDefined, "Undefined 'name' property!")

    val name = nameOpt.get.text
    var dependency: Dependency = null

    for (method <- methods) {
      val isEq = name.equals(method.getName)
      val capitalized = s"set${name.capitalize}"

      if (isEq || capitalized.equals(method.getName)) {
        require(dependency == null, s"ambiguous mapping for node $node, already bound dependency $dependency")
        // this is a plain setter, maybe even inherited
        // from the interface, doesn't matter
        val field = if (isEq) name else capitalized

        if (isReferenceType(attributes)) {
          dependency = Dependency(Right(BeanRef(attributes.get("ref").get.text)), Setter(field))
        } else {
          dependency = parsePrimitive(node, context, Setter(field))
        }
      }
    }
    require(dependency != null,
      s"""Not found suitable setter/interface
         |inject method for node $node""".stripMargin)
    dependency
  }

  private def parseConstructorDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val constructors = cl.getConstructors

    require(constructors.nonEmpty, s"No public constructors found for bean $cl")

    var matches = 0
    var foundConstructor: Constructor[_] = null

    for (constructor <- constructors) {
      if (matches == 0 || constructor.isAnnotationPresent(classOf[Autowiring])) {
        foundConstructor = constructor
        matches += 1
      }
    }

    require(matches == 1,
      s"""Found $matches constructors, annotated with ${classOf[Autowiring].getClass.getName}
         |annotation while only one is required""".stripMargin)

    val dependencies = for (param <- foundConstructor.getParameters) yield {
      val autowiring = param.getAnnotation[Autowiring](classOf[Autowiring])
      val id = extractId(param.getType, autowiring)
      val depType = typeMapping.get(param.getType.getSimpleName)

      if (depType.isDefined) {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.properties(id)), settings.Constructor)
      } else {
        Dependency(Right(BeanRef(id)), settings.Constructor)
      }
    }
    dependencies toList
  }

  private def resolveScannedBeans(scanSettings: Iterable[ScanSettings], context: Context) = {
    val packages = new mutable.MutableList[String]
    val urls = new util.ArrayList[URL]

    for (setting <- scanSettings) {
      packages += setting.pack
      urls.addAll(ClasspathHelper.forPackage(setting.pack))
    }

    val reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(urls)
      .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
      .filterInputsBy(new FilterBuilder().includePackage(packages: _*)))

    val beanDeclarations = new mutable.MutableList[BeanDeclaration]
    val scannableTypes = reflections.getTypesAnnotatedWith(classOf[Component], true) ++ reflections.getTypesAnnotatedWith(classOf[Service], true) ++
      reflections.getTypesAnnotatedWith(classOf[Controller], true) ++ reflections.getTypesAnnotatedWith(classOf[Repository], true)

    for (t <- scannableTypes) {
      beanDeclarations += createBeanDeclaration(t.asInstanceOf[Class[_ <: AnyRef]], context)
    }

    beanDeclarations
  }

  private def createBeanDeclaration(cl: Class[_ <: AnyRef], context: Context) = {
    val a = cl.getAnnotation(classOf[Component])
    val id = extractId(cl, a)
    val scope = if (cl.isAnnotationPresent(classOf[core.di.annotation.Singleton])) settings.Singleton else PerInject

    BeanDeclaration(id, cl, scope, parseDependencies(cl, context))
  }

  private def isReferenceType(attributes: MetaData) = attributes.get("ref").isDefined

  private def parsePrimitive(node: Elem, context: Context, scope: InjectScope) = {
    val attributes = node.attributes
    val variable = attributes.get("var")

    if (variable.isDefined) {
      require(attributes.length == 1,
        s"""Only one attribute is allowed but were ${attributes.length},
           |$attributes""".stripMargin)
      Dependency(Left(context.properties(variable.get.text)), scope)

    } else {
      val valueOpt = attributes.get("value")

      require(valueOpt.nonEmpty,
        s"""Not found attribute, named 'value'
           |in node $node""".stripMargin)

      val typeOpt = attributes.get("type")
      require(typeOpt.nonEmpty,
        s"""Not found attribute, named 'type'
           |in node $node""".stripMargin)

      val pair = typeMapping.getOrElse(typeOpt.get.text, throw new RuntimeException(s"Not found type for ${typeOpt.get.text}"))

      Dependency(Left(Property(pair.getWrappedClass, pair.transform(valueOpt.get.text))), scope)
    }
  }

  private def parseScope(node: Node) =
    node.attribute("scope") match {
      case Some(x) if x.text.equals("instance") => PerInject
      case None => settings.Singleton
    }

  private def extractId(argument: Class[_], annotation: Annotation) = {
    if (annotation == null) {
      BeanUtil.createBeanId(argument)

    } else {
      val rawId = annotation match {
        case a: Autowiring => a.named
        case c: Component => c.id
        case s: Service => s.id
        case c: Controller => c.id
        case r: Repository => r.id
      }

      if (rawId.isEmpty) BeanUtil.createBeanId(argument) else rawId
    }
  }

}

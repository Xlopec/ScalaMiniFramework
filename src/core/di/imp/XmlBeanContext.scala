package core.di.imp

import java.io.File
import java.lang.reflect.{Method, Modifier}
import java.util

import core.di.settings._
import core.di.{BeanContext, BeanFactory}

import scala.collection.JavaConverters._
import scala.xml.{Elem, MetaData, Node, XML}

/**
  * Created by Максим on 9/10/2017.
  *
  */

final class XmlBeanContext(file: File) extends BeanContext {
  require(file != null, "file == null")
  require(file.exists() && file.isFile && file.canRead,
    s"""Couldn't read xml config file ${file.getAbsolutePath}:
        |exists -${file.exists()},
        |is file -${file.isFile},
        |readable -${file.canRead}
   """.stripMargin)

  private lazy val declarations: Iterable[BeanDeclaration] = parseBeans()
  private var loadedClasses: Map[String, Class[_]] = collectClasses()

  override def getBeanFactory: BeanFactory = BeanFactoryImp.apply(declarations)

  private def parseBeans(): Iterable[BeanDeclaration] = {
    val beans = XML.loadFile(file) \ "bean"

    beans map (node => parseNode(node)) toList
  }

  private def parseNode(node: Node) = {
    val id = node \ "@id" text
    val classOf = findClass(node \ "@class" text).asInstanceOf[Class[AnyRef]]
    BeanDeclaration(id, classOf, parseDependencies(node, classOf))
  }

  private def parseDependencies(node: Node, classOf: Class[_]): List[Dependency] = {
    val constructorArgs = node \ "constructor-arg" map (node => parseConstructorDeps(node.asInstanceOf[Elem])) toList
    val methods = classOf.getMethods.filter(m => isInjectable(m))
    val setterArgs = node \ "property" map (node => parseSetterDeps(node.asInstanceOf[Elem], methods)) toList;
    constructorArgs ++ setterArgs
  }

  private def parseConstructorDeps(node: Elem) = {
    val attributes = node.attributes
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 1,
        s"""Invalid attributes length, should be 1 but were ${attributes.length}
            |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), CONSTRUCTOR)
    } else {
      parsePrimitive(node, CONSTRUCTOR)
    }
  }

  private def isInjectable(method: Method) =
    method.getParameterCount == 1 && (method.getModifiers & Modifier.PUBLIC) != 0 && method.getReturnType.getName.equals("void")

  private def parseSetterDeps(node: Elem, methods: Array[Method]): Dependency = {
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
          dependency = Dependency(Right(BeanRef(attributes.get("ref").get.text)), SETTER(field))
        } else {
          dependency = parsePrimitive(node, SETTER(field))
        }
      }
    }
    require(dependency != null,
      s"""Not found suitable setter/interface
          |inject method for node $node""".stripMargin)
    dependency
  }

  private def isReferenceType(attributes: MetaData) = attributes.get("ref").isDefined

  private def parsePrimitive(node: Elem, scope: InjectScope) = {
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
    Dependency(Left(PrimitiveType(pair.getWrappedClass, pair.transform(valueOpt.get.text))), scope)
  }

  private def findClass(classOf: String) = {
    loadedClasses.getOrElse(classOf, {
      // try to load class and add it
      // to the collection of loaded classes
      val cl = Class.forName(classOf)

      loadedClasses += cl.getName -> cl
      cl
    })
  }

  private def collectClasses() = {
    val classLoader = Thread.currentThread.getContextClassLoader
    var classLoaderCl = classLoader.getClass

    while (classLoaderCl != classOf[ClassLoader]) {
      classLoaderCl = classLoaderCl.getSuperclass.asSubclass(classOf[ClassLoader])
    }

    val clField = classLoaderCl.getDeclaredField("classes")

    clField.setAccessible(true)

    val vector = clField.get(classLoader).asInstanceOf[java.util.Vector[Class[_]]]
    //fixme remove wrapping!!1
    new util.ArrayList[Class[_]](vector).asScala.map(c => c.getName -> c).toMap[String, Class[_]]
  }

}

package core.di.imp

import java.lang.reflect.{Constructor, Field}
import java.net.URL
import java.util

import core.di.annotation._
import core.di.settings._
import core.di.{DeclarationParser, settings}
import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.xml.Node

final class AnnotationParser extends DeclarationParser {

  final case class BeanContext(cl: Class[_], settingsContext: Context, reflections: Reflections)

  override def parse(context: Context): Set[BeanDeclaration] = {
    val scanSettings = (context.xml \ "component-scan").map(parseScanSetting).toList

    resolveScannedBeans(scanSettings, context)
  }

  private def parseScanSetting(node: Node) = ScanSettings(node \ "@base-package" text)

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

    val beanDeclarations = mutable.Set[BeanDeclaration]()
    val scannableTypes = reflections.getTypesAnnotatedWith(classOf[Component], true) ++ reflections.getTypesAnnotatedWith(classOf[Service], true) ++
      reflections.getTypesAnnotatedWith(classOf[Controller], true) ++ reflections.getTypesAnnotatedWith(classOf[Repository], true)

    for (t <- scannableTypes) {
      beanDeclarations += createBeanDeclaration(BeanContext(t.asInstanceOf[Class[_ <: AnyRef]], context, reflections))
    }

    beanDeclarations toSet
  }

  private def createBeanDeclaration(context: BeanContext) = {
    val id = extractId(context.cl.getAnnotations, BeanUtil.createBeanId(context.cl))
    val scope = if (context.cl.isAnnotationPresent(classOf[core.di.annotation.Singleton])) settings.Singleton else PerInject

    BeanDeclaration(id, context.cl.asInstanceOf[Class[_ <: AnyRef]], scope, parseDependencies(context))
  }

  private def parseDependencies(context: BeanContext): List[Dependency] =
    parseConstructorDeps(context) ++ parseSetterDeps(context) ++ parseFieldDeps(context)

  private def parseConstructorDeps(context: BeanContext) = {
    val constructors = context.cl.getConstructors

    require(constructors.nonEmpty, s"No public constructors found for bean ${context.cl}")

    var annotations = 0
    var foundConstructor: Option[Constructor[_]] = Option.empty

    for (constructor <- constructors) {
      if (foundConstructor.isEmpty) {
        foundConstructor = Option(constructor)
      }

      if (constructor.isAnnotationPresent(classOf[Autowiring])) {
        annotations += 1
        foundConstructor = Option(constructor)
      }
    }

    require(constructors.length == 1 || (constructors.length > 1 && annotations == 1),
      s"""Accessible constructor is required and(or) annotated with ${classOf[Autowiring].getTypeName},
         |but only one for bean ${context.cl}""".stripMargin)

    if (foundConstructor.get.getParameters.length == 0) {
      List()
    } else {
      foundConstructor.get.getParameters
        .map(param => extractDependency(Option(param.getAnnotation[Autowiring](classOf[Autowiring])), BeanContext(param.getType, context.settingsContext, context.reflections), settings.Constructor))
        .toList
    }
  }

  private def parseFieldDeps(context: BeanContext) = {
    val filter = (f: Field) => {
      val isAnnotated = f.isAnnotationPresent(classOf[Autowiring])

      if (isAnnotated) {
        require(isInjectable(f),
          s"""cannot inject into field $f, field
             |should be made non-final, non-static and
             |shouldn't have native modifier
           """.stripMargin)
      }
      isAnnotated
    }

    context.cl.getDeclaredFields
      .filter(filter)
      .map(f => extractDependency(Option(f.getAnnotation[Autowiring](classOf[Autowiring])), BeanContext(f.getType, context.settingsContext, context.reflections), settings.Field(f)))
      .toList
  }

  private def parseSetterDeps(context: BeanContext) = {
    context.cl.getMethods
      .filter(m => m.isAnnotationPresent(classOf[Autowiring]))
      .map(m => extractDependency(Option(m.getAnnotation[Autowiring](classOf[Autowiring])), BeanContext(m.getParameterTypes()(0), context.settingsContext, context.reflections), Setter(m)))
      .toList
  }

  private def extractDependency(autowiring: Option[Autowiring], context: BeanContext, scope: InjectScope) = {
    var id: Option[String] = Option.empty

    if (autowiring.isDefined && autowiring.get.named().nonEmpty) {
      // short path, just extract id
      id = Option(extractId(autowiring))
    } else {
      require(!primitiveToWrapper.contains(context.cl.getTypeName),
        s"""Primitive type should always have fully qualified id.
           |See scope: $scope""".stripMargin)
      // long path, try to find single concrete class which inherits given one
      if (autowiring.isDefined || isConcrete(context.cl)) {
        // can generate id
        id = Option(BeanUtil.createBeanId(context.cl))
      } else {
        // in any other case scan for annotated subtypes of a given class
        val candidates = context.reflections.getSubTypesOf(context.cl)
          .filter(cl => isConcrete(cl) && scanAnnotation.intersect(cl.getDeclaredAnnotations.map(a => a.annotationType())).nonEmpty) toList

        id = candidates.length match {
          case 0 => throw new IllegalStateException(
            s"""No candidates found for inject for class ${context.cl}
               ||Scope: $scope""".stripMargin)
          case 1 => Option(BeanUtil.createBeanId(candidates.head))
          case _ => throw new IllegalStateException(
            s"""Found multiple candidates for class ${context.cl} to inject: $candidates.
               |Scope: $scope""".stripMargin)
        }
      }
    }
    require(id.isDefined, s"Couldn't find suitable candidate for injection for bean ${context.cl}, scope: $scope")
    createDependency(id.get, context, scope)
  }

  private def createDependency(id: String, context: BeanContext, scope: InjectScope) = {
    val depType = primitiveToWrapper.get(context.cl.getSimpleName)

    if (depType.isDefined) {
      Dependency(Left(context.settingsContext.variables(id)), scope)
    } else {
      Dependency(Right(BeanRef(id)), scope)
    }
  }

}

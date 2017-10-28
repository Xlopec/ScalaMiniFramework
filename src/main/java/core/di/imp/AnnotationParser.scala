package core.di.imp

import java.lang.reflect.Constructor
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

  override def parse(context: Context): Iterable[BeanDeclaration] = {
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

  private def parseDependencies(classOf: Class[_ <: AnyRef], context: Context): List[Dependency] =
    parseConstructorDeps(classOf, context) ++ parseSetterDeps(classOf, context)

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
        Dependency(Left(context.variables(id)), settings.Constructor)
      } else {
        Dependency(Right(BeanRef(id)), settings.Constructor)
      }
    }
    dependencies toList
  }

  private def parseSetterDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val dependencies = for (method <- cl.getMethods if method.isAnnotationPresent(classOf[Autowiring])) yield {
      require(isInjectable(method), s"Method $method isn't injectable")

      val cl = method.getParameterTypes()(0)
      val autowiring = method.getAnnotation[Autowiring](classOf[Autowiring])
      val id = extractId(cl, autowiring)
      val depType = typeMapping.get(cl.getSimpleName)

      if (depType.isEmpty) {
        Dependency(Right(BeanRef(id)), Setter(method.getName))
      } else {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.variables(id)), Setter(method.getName))
      }
    }

    dependencies toList
  }

}

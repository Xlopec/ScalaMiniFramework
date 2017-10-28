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
    val id = extractId(cl, Option(a))
    val scope = if (cl.isAnnotationPresent(classOf[core.di.annotation.Singleton])) settings.Singleton else PerInject

    BeanDeclaration(id, cl, scope, parseDependencies(cl, context))
  }

  private def parseDependencies(classOf: Class[_ <: AnyRef], context: Context): List[Dependency] =
    parseConstructorDeps(classOf, context) ++ parseSetterDeps(classOf, context) ++ parseFieldDeps(classOf, context)

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

    foundConstructor.getParameters
      .map(param => extractDependency(Option(param.getAnnotation[Autowiring](classOf[Autowiring])), param.getType, context, settings.Constructor))
      .toList
  }

  private def parseFieldDeps(cl: Class[_ <: AnyRef], context: Context) = {
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

    cl.getDeclaredFields
      .filter(filter)
      .map(f => extractDependency(Option(f.getAnnotation[Autowiring](classOf[Autowiring])), f.getType, context, settings.Field(f)))
      .toList
  }

  private def parseSetterDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val dependencies = for (method <- cl.getMethods if method.isAnnotationPresent(classOf[Autowiring])) yield {
      require(isInjectable(method), s"Method $method isn't injectable")

      val cl = method.getParameterTypes()(0)
      val autowiring = Option(method.getAnnotation[Autowiring](classOf[Autowiring]))
      val id = extractId(cl, autowiring)
      val depType = typeMapping.get(cl.getSimpleName)

      if (depType.isEmpty) {
        Dependency(Right(BeanRef(id)), Setter(method))
      } else {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.variables(id)), Setter(method))
      }
    }

    dependencies toList
  }

  private def extractDependency(autowiring: Option[Autowiring], cl: Class[_], context: Context, scope: InjectScope) = {
    val id = extractId(cl, autowiring)
    val depType = typeMapping.get(cl.getSimpleName)

    if (depType.isDefined) {
      // primitive type,
      // should be annotated
      require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
      Dependency(Left(context.variables(id)), scope)
    } else {
      Dependency(Right(BeanRef(id)), scope)
    }
  }

  /*private def checkCollisions(dependencies: Iterable[Dependency]) = {
    val eitherMap = dependencies.map(d => d.either).groupBy(e => e.isLeft) map { case (isLeft, coll) => {
      if (isLeft) {
        (isLeft, coll.map(e => e.left.get))
      } else {
        (isLeft, coll.map(e => e.right.get))
      }
    }
    }



    if (eitherMap.contains(true)) {
      eitherMap(true).map(e => e.asInstanceOf[Property]).groupBy(p => p.)
    }

    if (eitherMap.contains(false)) {

    }
  }*/

}

package core.di.imp

import java.io.File
import java.net.URL
import java.util

import core.di.annotation.{Component, Singleton}
import core.di.settings.{BeanDeclaration, ContextSettings, PerInject}
import core.di.{BeanContext, BeanFactory, ConfigParser, settings}
import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.mutable

/**
  * <p>
  * Represents an XML based bean context
  * </p>
  * Created by Максим on 9/10/2017.
  */
final class XmlBeanContext(parser: ConfigParser) extends BeanContext {
  require(parser != null)
  private val config = parser.parse()

  scanPackages(config)

  def this(file: File) = this(Parsers.createXmlParser(file))

  private lazy val factory = BeanFactoryImp.apply(config.declaration)

  override def getBeanFactory: BeanFactory = factory

  private def scanPackages(settings: ContextSettings) = {
    val packages = new mutable.MutableList[String]
    val urls = new util.ArrayList[URL]

    for (setting <- settings.scanSettings) {
      packages += setting.pack
      urls.addAll(ClasspathHelper.forPackage(setting.pack))
    }

    val reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(urls)
      .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
      .filterInputsBy(new FilterBuilder().includePackage(packages: _*)))

    val beanDeclarations = new mutable.MutableList[BeanDeclaration] ++ settings.declaration

    for (t <- reflections.getTypesAnnotatedWith(classOf[Component], true)) {
      beanDeclarations += createBeanDeclaration(t)
    }

    beanDeclarations
  }

  private def createBeanDeclaration(classOf: Class[_ <: AnyRef]) = {
    val a = classOf.getAnnotation(classOf[Component]).asInstanceOf[Component]
    val id = if (a.id() == null || a.id().isEmpty) classOf.getName else classOf.getName
    val scope = if (classOf.isAnnotationPresent(classOf[Singleton])) settings.Singleton else PerInject
    // TODO: finish this
    BeanDeclaration(id, classOf, scope, null)
  }

}

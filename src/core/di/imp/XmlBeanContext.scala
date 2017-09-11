package core.di.imp

import java.io.File
import java.{lang, util}

import core.di.settings._
import core.di.{BeanContext, BeanFactory}

import scala.collection.JavaConverters._
import scala.xml.{Elem, MetaData, Node, XML}

/**
  * Created by Максим on 9/10/2017.
  *
  */

sealed trait ValueTransformer {
  def transform(strVal: String): AnyRef

  def getWrappedClass: Class[_]
}

sealed case class StringPair() extends ValueTransformer {
  override def transform(strVal: String) = strVal

  override def getWrappedClass: Class[_] = classOf[lang.String]
}

sealed case class WrapperPair(primitive: Class[_], wrapper: Class[_]) extends ValueTransformer {
  override def transform(strVal: String) = wrapper.getMethod("valueOf", classOf[lang.String]).invoke(null, strVal)

  override def getWrappedClass: Class[_] = wrapper
}

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
  private val types: Map[String, ValueTransformer] = Map("int" -> WrapperPair(classOf[Int], classOf[Integer]),
    "boolean" -> WrapperPair(classOf[Boolean], classOf[lang.Boolean]),
    "byte" -> WrapperPair(classOf[Byte], classOf[lang.Byte]), "short" -> WrapperPair(classOf[Short], classOf[lang.Short]),
    "long" -> WrapperPair(classOf[Long], classOf[lang.Long]), "float" -> WrapperPair(classOf[Float], classOf[lang.Float]),
    "double" -> WrapperPair(classOf[Double], classOf[lang.Double]), "char" -> WrapperPair(classOf[Char], classOf[Character]),
    "void" -> WrapperPair(classOf[Void], classOf[lang.Void]), "String" -> StringPair())

  override def getBeanFactory: BeanFactory = BeanFactoryImp.apply(declarations)

  private def parseBeans(): Iterable[BeanDeclaration] = {
    val beans = XML.loadFile(file) \ "bean"

    beans map (node => parseNode(node)) toList
  }

  private def parseNode(node: Node) = {
    val id = node \ "@id" text
    val classOf = node \ "@class" text

    BeanDeclaration(id, findClass(classOf), parseDependencies(node))
  }

  <!--
  <bean id="..." class="...">
	<constructor-arg ref="..." />
	<property name="..." value="..." />
	<property name="..." ref="..." />
  </bean>
  -->

  private def parseDependencies(node: Node) = {
    val constructorArgs = node \ "constructor-arg" map (node => parseDependency(node.asInstanceOf[Elem].attributes, CONSTRUCTOR)) toList

    constructorArgs
  }

  private def parseDependency(attributes: MetaData, scope: InjectScope) = {
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 1, "Invalid bla bla1")

      Dependency(Right(BeanRef(refOpt.get.text)), scope)
    } else {
      val valueOpt = attributes.get("value")
      require(valueOpt.nonEmpty, "Invalid bla bla2")

      val typeOpt = attributes.get("type")
      require(typeOpt.nonEmpty, "Invalid bla bla3")

      val pair = types.getOrElse(typeOpt.get.text, throw new RuntimeException(s"Not found type for ${typeOpt.get.text}"))
      Dependency(Left(PrimitiveType(pair.getWrappedClass, pair.transform(valueOpt.get.text))), scope)
    }
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

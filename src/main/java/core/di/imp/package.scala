package core.di

import java.io.File
import java.lang
import java.lang.annotation.Annotation
import java.lang.reflect.{Method, Modifier}

import core.di.annotation._

import scala.xml.{Elem, XML}

/**
  * Created by Максим on 9/11/2017.
  */
package object imp {

  private[imp] val scanAnnotation = Array(classOf[Repository], classOf[Component], classOf[Service], classOf[Controller])

  private[imp] val typeMapping: Map[String, ValueTransformer] = Map("int" -> WrapperPair(classOf[Int], classOf[Integer]),
    "boolean" -> WrapperPair(classOf[Boolean], classOf[lang.Boolean]),
    "byte" -> WrapperPair(classOf[Byte], classOf[lang.Byte]), "short" -> WrapperPair(classOf[Short], classOf[lang.Short]),
    "long" -> WrapperPair(classOf[Long], classOf[lang.Long]), "float" -> WrapperPair(classOf[Float], classOf[lang.Float]),
    "double" -> WrapperPair(classOf[Double], classOf[lang.Double]), "char" -> WrapperPair(classOf[Char], classOf[Character]),
    "void" -> WrapperPair(classOf[Void], classOf[lang.Void]), "String" -> StringPair())

  private[imp] trait ValueTransformer {
    def transform(strVal: String): AnyRef

    def getWrappedClass: Class[_ <: AnyRef]
  }

  private[imp] final case class StringPair() extends ValueTransformer {
    override def transform(strVal: String): String = strVal

    override def getWrappedClass: Class[_ <: AnyRef] = classOf[lang.String]
  }

  private[imp] final case class WrapperPair(primitive: Class[_], wrapper: Class[_ <: AnyRef]) extends ValueTransformer {
    override def transform(strVal: String): AnyRef = wrapper.getMethod("valueOf", classOf[lang.String]).invoke(null, strVal)

    override def getWrappedClass: Class[_ <: AnyRef] = wrapper
  }

  private[imp] def extractId(argument: Class[_], annotation: Annotation) = {
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

  private[imp] def isInjectable(method: Method) =
    method.getParameterCount == 1 && (method.getModifiers & Modifier.PUBLIC) != 0 && method.getReturnType.getName.equals("void")


  def loadXml(file: File): Elem = {
    require(file != null, "file == null")
    require(file.exists() && file.isFile && file.canRead,
      s"""Couldn't read xml config file ${file.getAbsolutePath}:
         |exists -${file.exists()},
         |is file -${file.isFile},
         |readable -${file.canRead}
   """.stripMargin)

    XML.loadFile(file)
  }

}

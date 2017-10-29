package core.di

import java.io.File
import java.lang
import java.lang.annotation.Annotation
import java.lang.reflect.{Field, Method, Modifier}

import core.di.annotation._

import scala.xml.{Elem, XML}

/**
  * Created by Максим on 9/11/2017.
  */
package object imp {

  private[imp] val scanAnnotation = Array(classOf[Repository], classOf[Component], classOf[Service], classOf[Controller])

  private[imp] val primitiveToWrapper: Map[String, ValueTransformer] = Map("int" -> WrapperPair(classOf[Int], classOf[Integer]),
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

  private[imp] def extractId(annotation: Autowiring, alternative: => String) = {
    val rawId = annotation.named()
    if (rawId == null || rawId.isEmpty) alternative else rawId
  }

  private[imp] def extractId(annotation: Autowiring) : String =
    extractId(annotation, throw new IllegalStateException("shouldn't get here"))

  private[imp] def extractId(annotations: Iterable[Annotation], alternative: => String) = {
    val filtered = annotations.collect ({
      case a: Autowiring => a.named
      case c: Component => c.id
      case s: Service => s.id
      case c: Controller => c.id
      case r: Repository => r.id
    }).toArray

    require(filtered.length <= 1)

    if (filtered.isEmpty || filtered(0) == null || filtered(0).isEmpty) {
      alternative
    } else {
      filtered(0)
    }
  }

  private[imp] def extractId(annotations: Iterable[Annotation]) : String =
    extractId(annotations, throw new IllegalStateException("shouldn't get here"))

  private[imp] def isInjectable(method: Method) =
    method.getParameterCount == 1 && (method.getModifiers & Modifier.PUBLIC) != 0 && method.getReturnType.getName.equals("void")

  private[imp] def isInjectable(field: Field) =
    (field.getModifiers & (Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC)) == 0

  private[imp] def isConcrete(cl: Class[_]) = (cl.getModifiers & (Modifier.ABSTRACT | Modifier.INTERFACE)) == 0

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

package core.di

import java.lang

import core.di.annotation.{Component, Controller, Repository, Service}

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
    override def transform(strVal: String) = strVal

    override def getWrappedClass: Class[_ <: AnyRef] = classOf[lang.String]
  }

  private[imp] final case class WrapperPair(primitive: Class[_], wrapper: Class[_ <: AnyRef]) extends ValueTransformer {
    override def transform(strVal: String) = wrapper.getMethod("valueOf", classOf[lang.String]).invoke(null, strVal)

    override def getWrappedClass: Class[_ <: AnyRef] = wrapper
  }

}

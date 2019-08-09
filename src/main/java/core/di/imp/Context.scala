package core.di.imp

import core.di.settings.Property

import scala.xml.Elem

/**
  * Xml configuration context
  *
  * @param xml       root element of a parsed xml file
  * @param variables variables defined in the specified xml file
  */
final case class Context(xml: Elem, variables: Map[String, Property]) {
  require(xml != null, "xml undefined")
  require(variables != null, "variables undefined")
}

package core.di.imp

import java.io.File

import core.di.settings.Property
import core.di.{ConfigParser, DeclarationParser}

import scala.xml.{Elem, Node}

final class XmlConfigParser(file: File, parsers: Iterable[DeclarationParser]) extends ConfigParser(loadXml(file), parsers) {

  override protected def parseVariables(xml: Elem): Map[String, Property] = xml \ "property" map parseProperty toMap

  private def parseProperty(node: Node) = {
    val id = node \ "@id" text
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

    (id, Property(pair.getWrappedClass, pair.transform(valueOpt.get.text)))
  }

}

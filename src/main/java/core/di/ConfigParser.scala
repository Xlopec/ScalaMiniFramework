package core.di

import core.di.imp.Context
import core.di.settings._

import scala.xml.Elem

/**
  * Created by Максим on 9/11/2017.
  */
abstract class ConfigParser(xml: Elem, val parsers: Iterable[DeclarationParser]) {
  require(parsers != null && parsers.nonEmpty, "parsers weren't specified")

  def parse(): ContextSettings = {
    val context = Context(xml, parseVariables(xml))
    val declarations = for (parser <- parsers) yield parser.parse(context)

    ContextSettings(declarations.foldLeft(List[BeanDeclaration]())((prev, item) => prev ++ item.iterator))
  }

  protected def parseVariables(xml: Elem): Map[String, Property]

}

trait DeclarationParser {
  def parse(context: Context): Iterable[BeanDeclaration]
}
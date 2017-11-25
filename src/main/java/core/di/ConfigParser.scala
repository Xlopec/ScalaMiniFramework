package core.di

import core.di.imp.Context
import core.di.settings._

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by Максим on 9/11/2017.
  */
abstract class ConfigParser(xml: Elem, val parsers: Iterable[DeclarationParser]) {
  require(parsers != null && parsers.nonEmpty, "parsers weren't specified")

  def parse(): ContextSettings = {
    val context = Context(xml, parseVariables(xml))
    val declarations = mutable.Set[BeanDeclaration]()

    def collectCollisions(collector: mutable.Set[BeanDeclaration], collected: Set[BeanDeclaration]) = {
      collector map (d => d.id) intersect (collected map (b => b.id))
    }

    for (parser <- parsers) {
      val parsed = parser.parse(context)

      val collisions = collectCollisions(declarations, parsed.toSet)

      require(collisions.isEmpty,
        s"""Found collisions while parsing bean declarations,
           |please, check bean ${collisions.mkString}
        """.stripMargin)

      declarations ++= parser.parse(context)
    }

    ContextSettings(declarations)
  }

  protected def parseVariables(xml: Elem): Map[String, Property]

}

trait DeclarationParser {
  def parse(context: Context): Set[BeanDeclaration]
}
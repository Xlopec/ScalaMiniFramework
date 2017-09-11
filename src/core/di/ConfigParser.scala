package core.di

import java.io.File

import core.di.settings.BeanDeclaration

/**
  * Created by Максим on 9/11/2017.
  */
trait ConfigParser {
  def parse(): Iterable[BeanDeclaration]
}

object Parser {
  def createParser() = ???
}

final class XmlParser(file: File) extends ConfigParser {
  override def parse(): Iterable[BeanDeclaration] = ???
}

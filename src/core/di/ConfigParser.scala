package core.di

import core.di.settings._

/**
  * Created by Максим on 9/11/2017.
  */
trait ConfigParser {
  def parse(): Iterable[BeanDeclaration]
}
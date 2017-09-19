package core.di.imp

import java.io.File

import core.di.{BeanContext, ConfigParser}

/**
  * <p>
  * Represents an XML based bean context
  * </p>
  * Created by Максим on 9/10/2017.
  */
final class XmlBeanContext(parser: ConfigParser) extends BeanContext {
  require(parser != null)
  def this(file: File) = this(Parsers.createXmlParser(file))

  private lazy val factory = BeanFactoryImp.apply(parser.parse())

  override def getBeanFactory = factory

}

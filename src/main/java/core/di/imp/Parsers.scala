package core.di.imp

import java.io.File

import core.di.{ConfigParser, DeclarationParser}

import scala.collection.mutable

/**
  * <p>
  * Factory to instantiate configuration parsers
  * </p>
  * Created by Максим on 9/19/2017.
  */
object Parsers {
  /**
    * Creates default XML-based configuration parser. By default,
    * annotation-based scanning is enabled
    *
    * @param file an xml file to parse. Shouldn't be null, must exist
    *             and be readable
    * @return a new instance of ConfigParser
    */
  def createXmlParser(file: File): ConfigParser = createXmlParser(file, Props())

  /**
    * Creates default XML-based configuration parser. By default,
    * annotation-based scanning is enabled
    *
    * @param file  an xml file to parse. Shouldn't be null, must exist
    *              and be readable
    * @param props parsing properties
    * @return a new instance of ConfigParser
    */
  def createXmlParser(file: File, props: Props): ConfigParser = {
    val parsers = mutable.ListBuffer[DeclarationParser](new XmlParser)

    if (props.scanEnabled) {
      parsers += new AnnotationParser
    }
    new XmlConfigParser(file, parsers)
  }

}

final case class Props(scanEnabled: Boolean = true)

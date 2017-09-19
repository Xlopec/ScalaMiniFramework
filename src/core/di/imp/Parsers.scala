package core.di.imp

import java.io.File

/**
  * <p>
  * Factory to instantiate configuration parsers
  * </p>
  * Created by Максим on 9/19/2017.
  */
object Parsers {
  /**
    * Creates default XML-based configuration parser
    *
    * @param file an xml file to parse. Shouldn't be null, must exist
    *             and be readable
    * @return a new instance of ConfigParser
    */
  def createXmlParser(file: File) = new XmlParser(file)
}

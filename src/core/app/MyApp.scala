package core.app

import java.io.File

import core.di.imp.XmlBeanContext

/**
  * Created by Максим on 9/10/2017.
  */
object MyApp extends App {

  override def main(args: Array[String]): Unit = {
    println("Hello")

    val context = new XmlBeanContext(new File("D:\\Workspace Intellij Idea\\ScalaFramework\\src\\resources\\GS_SpringXMLConfig.xml"))

    val factory = context.getBeanFactory
  }

}

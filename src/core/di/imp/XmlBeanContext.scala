package core.di.imp

import java.io.File
import java.util

import core.di.settings._
import core.di.{BeanContext, BeanFactory}

import scala.collection.JavaConverters._
import scala.xml.{Elem, MetaData, Node, XML}

/**
  * Created by Максим on 9/10/2017.
  *
  */

final class XmlBeanContext(file: File) extends BeanContext {
  require(file != null, "file == null")
  require(file.exists() && file.isFile && file.canRead,
    s"""Couldn't read xml config file ${file.getAbsolutePath}:
        |exists -${file.exists()},
        |is file -${file.isFile},
        |readable -${file.canRead}
   """.stripMargin)

  private lazy val declarations: Iterable[BeanDeclaration] = parseBeans()
  private var loadedClasses: Map[String, Class[_]] = collectClasses()

  override def getBeanFactory: BeanFactory = BeanFactoryImp.apply(declarations)

  private def parseBeans(): Iterable[BeanDeclaration] = {
    val beans = XML.loadFile(file) \ "bean"

    beans map (node => parseNode(node)) toList
  }

  private def parseNode(node: Node) = {
    val id = node \ "@id" text
    val classOf = node \ "@class" text

    BeanDeclaration(id, findClass(classOf), parseDependencies(node))
  }

  private def parseDependencies(node: Node): List[Dependency] = {
    val constructorArgs = node \ "constructor-arg" map (node => parseConstructorDeps(node.asInstanceOf[Elem])) toList
    val setterArgs = node \ "property" map (node => parseSetterDeps(node.asInstanceOf[Elem])) toList;
    constructorArgs ++ setterArgs
  }

  private def parseConstructorDeps(node: Elem) = {
    val attributes = node.attributes
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 1,
        s"""Invalid attributes length, should be 1 but were ${attributes.length}
            |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), CONSTRUCTOR)
    } else {
      parsePrimitive(node, CONSTRUCTOR)
    }
  }

  private def parseSetterDeps(node: Elem) = {
    val attributes = node.attributes
    val nameOpt = attributes.get("name")
    require(nameOpt.isDefined, "Undefined 'name' property!")

    val scope = SETTER(nameOpt.get.text)
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 2,
        s"""Invalid attributes length, should be 2 but were ${attributes.length}
            |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), scope)
    } else {
      parsePrimitive(node, scope)
    }
  }

  private def parseInterfaceDeps(node: Elem, classOf: Class[_]) = {
    val attributes = node.attributes
    val nameOpt = attributes.get("name")
    require(nameOpt.isDefined, "Undefined 'name' property!")

    val name = nameOpt.get.text
    val interfaces = classOf.getAnnotatedInterfaces.map(t => t.getClass)

    for (method <- classOf.getMethods) {

      if (s"set$name.capitalize".equals(method.getName)) {
        // this is a plain setter

      }
    }

    val scope = SETTER(nameOpt.get.text)
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 2,
        s"""Invalid attributes length, should be 2 but were ${attributes.length}
            |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), scope)
    } else {
      parsePrimitive(node, scope)
    }
  }

  private def isReferenceType(attributes: MetaData) = attributes.get("ref").isDefined

  private def checkValidAttrs(node: Elem, scope: InjectScope) = {
    val attributes = node.attributes

    def error = {
      if (isReferenceType(attributes)) {
        scope match {
          // only ref required
          case CONSTRUCTOR if attributes.length != 1 => Some(1)
          // ref and name required
          case SETTER(_) | INTERFACE if attributes.length != 2 => Some(2)
          case _ => None
        }
      } else {
        scope match {
          // type and value required
          case CONSTRUCTOR if attributes.length != 2 => Some(2)
          // type, value and name are required
          case SETTER(_) | INTERFACE if attributes.length != 3 => Some(3)
          case _ => None
        }
      }
    }

    if (error.isDefined) {
      throw new IllegalStateException(
        s"""Invalid attributes length, should be ${error.get} but were ${attributes.length}
            |in node $node""".stripMargin)
    }
  }

  private def parsePrimitive(node: Elem, scope: InjectScope) = {
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
    Dependency(Left(PrimitiveType(pair.getWrappedClass, pair.transform(valueOpt.get.text))), scope)
  }

  private def findClass(classOf: String) = {
    loadedClasses.getOrElse(classOf, {
      // try to load class and add it
      // to the collection of loaded classes
      val cl = Class.forName(classOf)

      loadedClasses += cl.getName -> cl
      cl
    })
  }

  private def collectClasses() = {
    val classLoader = Thread.currentThread.getContextClassLoader
    var classLoaderCl = classLoader.getClass

    while (classLoaderCl != classOf[ClassLoader]) {
      classLoaderCl = classLoaderCl.getSuperclass.asSubclass(classOf[ClassLoader])
    }

    val clField = classLoaderCl.getDeclaredField("classes")

    clField.setAccessible(true)

    val vector = clField.get(classLoader).asInstanceOf[java.util.Vector[Class[_]]]
    //fixme remove wrapping!!1
    new util.ArrayList[Class[_]](vector).asScala.map(c => c.getName -> c).toMap[String, Class[_]]
  }

}

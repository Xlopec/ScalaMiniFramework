package core.di.imp

import java.lang.reflect.{Constructor, Method}

import core.di.annotation.{Singleton => _, _}
import core.di.settings._
import core.di.{DeclarationParser, settings}

import scala.language.postfixOps
import scala.xml.{Elem, MetaData, Node}

/**
  * Created by Максим on 9/19/2017.
  */
final class XmlParser extends DeclarationParser {

  override def parse(context: Context): Iterable[BeanDeclaration] =
    context.xml \ "bean" map (node => parseBeanDeclaration(node, context)) toList

  private def parseBeanDeclaration(node: Node, context: Context) = {
    val id = node \ "@id" text
    val classOf = Class.forName(node \ "@class" text).asInstanceOf[Class[AnyRef]]

    BeanDeclaration(id, classOf, parseScope(node), parseDependencies(node, classOf, context))
  }

  private def parseDependencies(node: Node, classOf: Class[_], context: Context): List[Dependency] = {
    val constructorDeps = node \ "constructor-arg" map (node => parseConstructorDeps(node.asInstanceOf[Elem], context)) toList
    val methods = classOf.getMethods.filter(m => isInjectable(m))
    val setterDeps = node \ "property" map (node => parseSetterDeps(node.asInstanceOf[Elem], methods, context)) toList

    constructorDeps ++ setterDeps
  }

  private def parseConstructorDeps(node: Elem, context: Context) = {
    val attributes = node.attributes
    val refOpt = attributes.get("ref")

    if (refOpt.nonEmpty) {
      require(attributes.length == 1,
        s"""Invalid attributes length, should be 1 but were ${attributes.length}
           |in node $node""".stripMargin)

      Dependency(Right(BeanRef(refOpt.get.text)), settings.Constructor)
    } else {
      parsePrimitive(node, context, settings.Constructor)
    }
  }

  private def parseSetterDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val dependencies = for (method <- cl.getMethods if method.isAnnotationPresent(classOf[Autowiring])) yield {
      require(isInjectable(method), s"Method $method isn't injectable")

      val cl = method.getParameterTypes()(0)
      val autowiring = method.getAnnotation[Autowiring](classOf[Autowiring])
      val depType = primitiveToWrapper.get(cl.getSimpleName)
      val id = extractId(autowiring, BeanUtil.createBeanId(cl))

      if (depType.isEmpty) {
        Dependency(Right(BeanRef(id)), Setter(method))
      } else {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.variables(id)), Setter(method))
      }
    }

    dependencies toList
  }

  private def parseSetterDeps(node: Elem, methods: Array[Method], context: Context): Dependency = {
    val attributes = node.attributes
    val nameOpt = attributes.get("name")
    require(nameOpt.isDefined, "Undefined 'name' property!")

    val name = nameOpt.get.text
    var dependency: Dependency = null

    for (method <- methods) {
      val isEq = name.equals(method.getName)
      val capitalized = s"set${name.capitalize}"

      if (isEq || capitalized.equals(method.getName)) {
        require(dependency == null, s"ambiguous mapping for node $node, already bound dependency $dependency")
        // this is a plain setter, maybe even inherited
        // from the interface, doesn't matter
        val field = if (isEq) name else capitalized

        if (isReferenceType(attributes)) {
          dependency = Dependency(Right(BeanRef(attributes.get("ref").get.text)), Setter(method))
        } else {
          dependency = parsePrimitive(node, context, Setter(method))
        }
      }
    }
    require(dependency != null,
      s"""Not found suitable setter/interface
         |inject method for node $node""".stripMargin)
    dependency
  }

  private def parseConstructorDeps(cl: Class[_ <: AnyRef], context: Context) = {
    val constructors = cl.getConstructors

    require(constructors.nonEmpty, s"No public constructors found for bean $cl")

    var matches = 0
    var foundConstructor: Constructor[_] = null

    for (constructor <- constructors) {
      if (matches == 0 || constructor.isAnnotationPresent(classOf[Autowiring])) {
        foundConstructor = constructor
        matches += 1
      }
    }

    require(matches == 1,
      s"""Found $matches constructors, annotated with ${classOf[Autowiring].getClass.getName}
         |annotation while only one is required""".stripMargin)

    val dependencies = for (param <- foundConstructor.getParameters) yield {
      val autowiring = param.getAnnotation[Autowiring](classOf[Autowiring])
      val id = extractId(autowiring, BeanUtil.createBeanId(cl))
      val depType = primitiveToWrapper.get(param.getType.getSimpleName)

      if (depType.isDefined) {
        // primitive type,
        // should be annotated
        require(autowiring != null, s"Primitive argument should be annotated with ${classOf[Autowiring]}")
        Dependency(Left(context.variables(id)), settings.Constructor)
      } else {
        Dependency(Right(BeanRef(id)), settings.Constructor)
      }
    }
    dependencies toList
  }

  private def isReferenceType(attributes: MetaData) = attributes.get("ref").isDefined

  private def parsePrimitive(node: Elem, context: Context, scope: InjectScope) = {
    val attributes = node.attributes
    val variable = attributes.get("var")

    if (variable.isDefined) {
      require(attributes.length == 1,
        s"""Only one attribute is allowed but were ${attributes.length},
           |$attributes""".stripMargin)
      Dependency(Left(context.variables(variable.get.text)), scope)

    } else {
      val valueOpt = attributes.get("value")

      require(valueOpt.nonEmpty,
        s"""Not found attribute, named 'value'
           |in node $node""".stripMargin)

      val typeOpt = attributes.get("type")
      require(typeOpt.nonEmpty,
        s"""Not found attribute, named 'type'
           |in node $node""".stripMargin)

      val pair = primitiveToWrapper.getOrElse(typeOpt.get.text, throw new RuntimeException(s"Not found type for ${typeOpt.get.text}"))

      Dependency(Left(Property(pair.getWrappedClass, pair.transform(valueOpt.get.text))), scope)
    }
  }

  private def parseScope(node: Node) =
    node.attribute("scope") match {
      case Some(x) if x.text.equals("instance") => PerInject
      case _ => settings.Singleton
    }

}

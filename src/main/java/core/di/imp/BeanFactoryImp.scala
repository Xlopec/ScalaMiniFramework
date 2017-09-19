package core.di.imp

import java.lang.reflect
import java.lang.reflect.Constructor

import core.di.{BeanFactory, settings}
import core.di.settings._

import scala.collection.mutable

object BeanFactoryImp {
  def apply(declarations: Iterable[BeanDeclaration]): BeanFactoryImp = new BeanFactoryImp(declarations)
}

/**
  * Created by Максим on 9/10/2017.
  */
final class BeanFactoryImp(declarations: Iterable[BeanDeclaration]) extends BeanFactory {

  private val idToDeclaration = declarations.map(v => v.id -> v).toMap
  private val classToDeclaration = declarations.map(v => v.classOf -> v).toMap[Class[_ <: AnyRef], BeanDeclaration]
  private val singletones = new mutable.HashMap[Class[_ <: AnyRef], Any]()

  override def instantiate[T <: AnyRef](c: Class[T]): T = {
    instantiate(c, new mutable.HashSet[String]())
  }

  override def instantiate[T <: AnyRef](id: String): T = {
    instantiate(id, new mutable.HashSet[String]())
  }

  private def instantiate[T <: AnyRef](c: Class[T], beanChain: mutable.Set[String]): T = {
    require(classToDeclaration.contains(c), s"Bean of class $c wasn't found, probably, you're missing bean declaration?")
    val declaration = classToDeclaration(c)

    if (declaration.scope == Singleton) {
      val classOf = declaration.classOf

      singletones.getOrElseUpdate(classOf, instantiate(c, classToDeclaration(c), beanChain)).asInstanceOf[T]
    } else {
      instantiate(c, classToDeclaration(c), beanChain)
    }
  }

  private def instantiate[T <: AnyRef](id: String, beanChain: mutable.Set[String]): T = {
    require(idToDeclaration.contains(id), s"Bean with id $id wasn't found, probably, you're missing bean declaration?")
    val declaration = idToDeclaration(id)

    if (declaration.scope == Singleton) {
      val classOf = declaration.classOf

      singletones.getOrElseUpdate(classOf, instantiate(classOf.asInstanceOf[Class[T]], declaration, beanChain)).asInstanceOf[T]
    } else {
      instantiate(classOf.asInstanceOf[Class[T]], declaration, beanChain)
    }
  }

  private def instantiate[T <: AnyRef](c: Class[T], declaration: BeanDeclaration, beanChain: mutable.Set[String]) = {
    val bean = instantiateForConstructor(c, declaration, beanChain)
    // injects declared dependencies
    // through declared setters
    inject(bean, declaration)
    bean
  }

  private def instantiateForConstructor[T <: AnyRef](c: Class[T], declaration: BeanDeclaration, beanChain: mutable.Set[String]): T = {
    require(!hasCyclicDependency(declaration, beanChain),
      s"""Cyclic dependency was found for constructor of bean: ${declaration.id},
          |the dependency path was: ${beanChain.mkString("->")}
       """.stripMargin)

    beanChain += declaration.id

    val declared = declaration.dependencies.filter(dep => dep.scope == settings.Constructor)
    val matchingConstructors = declaration.classOf.getConstructors.filter(constructor => constructor.getParameterCount == declared.length)

    require((declared.nonEmpty && matchingConstructors.length >= 1)
      // generated or default constructor case
      || (declared.isEmpty && matchingConstructors.length <= 1),
      s"""No suitable constructor was found for bean of class ${declaration.classOf} and of id ${declaration.id}""".stripMargin)

    if (declared.isEmpty) {
      // no declared dependencies, go to a short path
      declaration.classOf.newInstance().asInstanceOf[T]
    } else {
      // bean has declared dependencies, go to a long path
      val args = declared.map(dep => dep.either match {
        case Left(PrimitiveType(_, value)) => value
        case Right(BeanRef(id)) => instantiate(id, beanChain)
      })

      var matchingConstructor: Option[reflect.Constructor[T]] = Option.empty

      for (constructor <- matchingConstructors) {
        val argsMatching = isMatchingArgs(args, constructor)

        require(argsMatching && matchingConstructor.isEmpty,
          s"""At least two constructors can be used to instantiate bean of class $c,
              |the first - ${matchingConstructor.get.getParameterTypes}
              |and the second one - ${constructor.getParameterTypes}""".stripMargin)

        if (argsMatching) {
          matchingConstructor = Option.apply(constructor.asInstanceOf[reflect.Constructor[T]])
        }
      }
      require(matchingConstructor.isDefined, s"Couldn't find suitable constructor for bean $c")
      matchingConstructor.get.newInstance(args: _*)
    }
  }

  private def inject[T](bean: T, declaration: BeanDeclaration): Unit = {
    val declared = declaration.dependencies.filter(dep => dep.scope != settings.Constructor)

    for (dependency <- declared) {
      val args = dependency.either match {
        case Left(PrimitiveType(classOf, value)) => (classOf, value)
        case Right(BeanRef(id)) => val instance = instantiate(id); (instance.getClass, instance)
      }

      bean.getClass.getMethod(dependency.scope.asInstanceOf[Setter].field, args._1).invoke(bean, args._2)
    }
  }

  private def isMatchingArgs(toCheckArgs: List[AnyRef], real: Constructor[_]) = {
    val realArgs = real.getParameterTypes

    def isConstructorArgsMatching(i: Int): Boolean = {
      if (i == real.getParameterCount - 1) true
      else realArgs(i).isAssignableFrom(toCheckArgs(i).getClass) && isConstructorArgsMatching(i + 1)
    }
    isConstructorArgsMatching(0)
  }

  private def hasCyclicDependency(declaration: BeanDeclaration, beanChain: mutable.Set[String]) =
    declaration.dependencies.filter(dep => dep.either.isRight).map(dep => dep.either.right.get.id).intersect(beanChain.toSeq).nonEmpty

}

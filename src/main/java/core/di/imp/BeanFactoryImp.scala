package core.di.imp

import java.lang.reflect
import java.lang.reflect.{Constructor, Modifier}

import core.di.annotation.Autowiring
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
      val cl = declaration.classOf

      singletones.synchronized {
        singletones.getOrElseUpdate(cl, instantiate(c, classToDeclaration(c), beanChain)).asInstanceOf[T]
      }
    } else {
      instantiate(c, classToDeclaration(c), beanChain)
    }
  }

  private def instantiate[T <: AnyRef](id: String, beanChain: mutable.Set[String]): T = {
    require(idToDeclaration.contains(id), s"Bean with id $id wasn't found, probably, you're missing bean declaration?")
    val declaration = idToDeclaration(id)
    val cl = declaration.classOf

    if (declaration.scope == Singleton) {

      singletones.synchronized {
        singletones.getOrElseUpdate(cl, instantiate(cl.asInstanceOf[Class[T]], declaration, beanChain)).asInstanceOf[T]
      }
    } else {
      instantiate(cl.asInstanceOf[Class[T]], declaration, beanChain)
    }
  }

  private def instantiate[T <: AnyRef](c: Class[T], declaration: BeanDeclaration, beanChain: mutable.Set[String]): T = {
    val bean = instantiateForConstructor(c, declaration, beanChain)
    val dependencyResolver = (dependency: Dependency) => {
      dependency.either match {
        case Left(Property(_, value)) => value
        case Right(BeanRef(id)) => instantiate(id)
      }
    }
    // injects declared dependencies
    // through declared setters
    injectSetters(bean, declaration, dependencyResolver)
    injectFields(bean, declaration, dependencyResolver)
    bean
  }

  private def instantiateForConstructor[T <: AnyRef](c: Class[T], declaration: BeanDeclaration, beanChain: mutable.Set[String]): T = {
    require(!Modifier.isAbstract(c.getModifiers), s"Cannot instantiate abstract class $c, bean ${declaration.id}")
    require(!hasCyclicDependency(declaration, beanChain),
      s"""Cyclic dependency was found for constructor of bean: ${declaration.id},
         |the dependency path was: ${beanChain.mkString("->")}
       """.stripMargin)

    beanChain += declaration.id

    val declared = declaration.dependencies.filter(dep => dep.scope == settings.Constructor)
    val matchingConstructors = declaration.classOf.getConstructors
      .filter(constructor => constructor.getParameterCount == declared.length && Modifier.isPublic(constructor.getModifiers))

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
        case Left(Property(_, value)) => value
        case Right(BeanRef(id)) => instantiate(id, beanChain)
      })

      val matchingConstructor = findMatchingConstructor(matchingConstructors, args)

      require(matchingConstructor.isDefined, s"Couldn't find suitable constructor for bean $c")
      matchingConstructor.get.newInstance(args: _*)
    }
  }

  private def findMatchingConstructor[T <: AnyRef](constructors: Array[Constructor[_]], args: List[AnyRef]) = {

    def loop(i: Int, previous: Option[Constructor[T]]): Option[Constructor[T]] = {
      if (i == constructors.length) {
        previous
      } else {
        val constructor = constructors(i)
        val argsMatching = isMatchingArgs(args, constructor)

        if (argsMatching) {
          if (previous.isDefined) {
            // previous constructor shouldn't be annotated;
            // in this case only one annotated constructor is acceptable
            require(!previous.get.isAnnotationPresent(classOf[Autowiring])
              && constructor.isAnnotationPresent(classOf[Autowiring]),
              s"""At least two constructors can be used to instantiate bean of class,
                 |the first - ${previous.get.getParameterTypes.toList}
                 |and the second one - ${constructor.getParameterTypes.toList}.
                 |Use ${classOf[Autowiring].getTypeName} annotation to choose which one to use,
                 |but only once""".stripMargin)
            // we've found constructor to inject,
            // we can ignore all other
            Option.apply(constructor.asInstanceOf[reflect.Constructor[T]])
          } else {
            loop(i + 1, Option.apply(constructor.asInstanceOf[reflect.Constructor[T]]))
          }
        } else {
          loop(i + 1, previous)
        }
      }
    }

    loop(0, Option.empty)
  }

  private def injectSetters[T](bean: T, declaration: BeanDeclaration, resolver: (Dependency => AnyRef)): Unit = {
    val setterDeps = declaration.dependencies collect {
      case d: Dependency if (d.scope match {
        case Setter(_) => true;
        case _ => false
      }) => d
    }

    for (dependency <- setterDeps) {
      dependency.scope.asInstanceOf[settings.Setter].method.invoke(bean, resolver(dependency))
    }
  }

  private def injectFields[T](bean: T, declaration: BeanDeclaration, resolver: (Dependency => AnyRef)): Unit = {
    val fieldDeps = declaration.dependencies collect {
      case d: Dependency if (d.scope match {
        case Field(_) => true;
        case _ => false
      }) => d
    }

    for (dependency <- fieldDeps) {
      val field = dependency.scope.asInstanceOf[settings.Field].field
      val isAccessible = field.isAccessible

      if (!isAccessible) {
        field.setAccessible(true)
      }

      try {
        val fieldVal = Option(field.get(bean))
        // after constructor and setter dependencies were injected
        // we should inject instance fields. If field value is present,
        // then it was injected before, which shouldn't happen.
        require(fieldVal.isEmpty,
          s"""Field of class ${bean.getClass} was already set either via constructor or via setter
             |or has a default value
           """.stripMargin)
        field.set(bean, resolver(dependency))
      } finally {
        if (!isAccessible) {
          field.setAccessible(isAccessible)
        }
      }
    }
  }

  private def isMatchingArgs(toCheckArgs: List[AnyRef], real: Constructor[_]) = {
    val realArgs = real.getParameterTypes

    def wrap(i: Int) = primitiveToWrapper get realArgs(i).getTypeName map (v => v.getWrappedClass) getOrElse realArgs(i)

    def isConstructorArgsMatching(i: Int): Boolean = {
      if (i == real.getParameterCount - 1) true
      else wrap(i).isAssignableFrom(toCheckArgs(i).getClass) && isConstructorArgsMatching(i + 1)
    }

    isConstructorArgsMatching(0)
  }

  private def hasCyclicDependency(declaration: BeanDeclaration, beanChain: mutable.Set[String]) =
    declaration.dependencies.filter(dep => dep.either.isRight).map(dep => dep.either.right.get.id).intersect(beanChain.toSeq).nonEmpty

}

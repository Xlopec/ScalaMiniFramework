package core.di.imp

import java.lang.reflect
import java.lang.reflect.Constructor

import core.di.BeanFactory
import core.di.settings.{BeanDeclaration, BeanRef, CONSTRUCTOR, PrimitiveType}

object BeanFactoryImp {
  def apply(declarations: Iterable[BeanDeclaration]): BeanFactoryImp = new BeanFactoryImp(declarations)
}

/**
  * Created by Максим on 9/10/2017.
  */
final class BeanFactoryImp(declarations: Iterable[BeanDeclaration]) extends BeanFactory {

  private val idToDeclaration = declarations.map(v => v.id -> v).toMap
  private val classToDeclaration = declarations.map(v => v.classOf -> v).toMap[Class[_], BeanDeclaration]

  override def instantiate[T](c: Class[T]): T = {
    require(classToDeclaration.contains(c), s"Bean of class $c wasn't found, probably, you're missing bean declaration?")

    instantiate(c, classToDeclaration(c))
  }

  override def instantiate[T](id: String): T = {
    require(idToDeclaration.contains(id), s"Bean with id $id wasn't found, probably, you're missing bean declaration?")

    idToDeclaration(id).classOf.newInstance().asInstanceOf[T]
  }

  private def instantiate[T](c: Class[T], bean: BeanDeclaration): T = {
    val declared = bean.dependencies.filter(dep => dep.scope == CONSTRUCTOR)
    val matchingConstructors = bean.classOf.getConstructors.filter(constructor => constructor.getParameterCount == declared.length)

    require((declared.nonEmpty && matchingConstructors.length >= 1)
      // generated or default constructor case
      || (declared.isEmpty && matchingConstructors.length <= 1),
      s"""No suitable constructor was found for bean|of class ${bean.classOf} and of id ${bean.id}""".stripMargin)

    if (declared.isEmpty) {
      bean.classOf.newInstance().asInstanceOf[T]
    } else {
      val args = declared.map(dep => dep.either match {
        case Left(PrimitiveType(_, value)) => value
        case Right(BeanRef(id)) => instantiate(id)
      })

      var matchingConstructor: Option[reflect.Constructor[T]] = Option.empty

      for (constructor <- matchingConstructors) {
        val argsMatching = matchingArgs(args, constructor)

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

  private def matchingArgs(toCheckArgs: List[AnyRef], real: Constructor[_]) = {
    val realArgs = real.getParameterTypes

    def isConstructorArgsMatching(i: Int): Boolean = {
      if (i == real.getParameterCount - 1) true
      else realArgs(i).isAssignableFrom(toCheckArgs(i).getClass) && isConstructorArgsMatching(i + 1)
    }
    isConstructorArgsMatching(0)
  }

}

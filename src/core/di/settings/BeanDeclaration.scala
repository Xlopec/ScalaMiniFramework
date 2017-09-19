package core.di.settings

/**
  * Created by Максим on 9/10/2017.
  */
object BeanDeclaration

final case class BeanDeclaration(id: String, classOf: Class[_ <: AnyRef], scope: BeanScope, dependencies: List[Dependency])

final case class PrimitiveType(classOf: Class[_], value: AnyRef)

final case class Dependency(either: Either[PrimitiveType, BeanRef], scope: InjectScope)

final case class BeanRef(id: String)

sealed trait InjectScope

case object Constructor extends InjectScope

object Setter

final case class Setter(field: String) extends InjectScope

sealed trait BeanScope

object Singleton extends BeanScope

object PerInject extends BeanScope
package core.di.settings

/**
  * Created by Максим on 9/10/2017.
  */
object BeanDeclaration

final case class BeanDeclaration(id: String, classOf: Class[_ <: AnyRef], dependencies: List[Dependency])

final case class PrimitiveType(classOf: Class[_], value: AnyRef)

final case class Dependency(either: Either[PrimitiveType, BeanRef], scope: InjectScope)

final case class BeanRef(id: String)

sealed trait InjectScope

case object CONSTRUCTOR extends InjectScope

object SETTER

final case class SETTER(field: String) extends InjectScope
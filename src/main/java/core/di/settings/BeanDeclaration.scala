package core.di.settings

/**
  * <p>
  * This source file contains bean declaration settings
  * classes
  * </p>
  * Created by Максим on 9/10/2017.
  */
object BeanDeclaration

final case class BeanDeclaration(id: String, classOf: Class[_ <: AnyRef], scope: BeanScope, dependencies: List[Dependency]) {
  require(id != null && !id.isEmpty, "undefined bean id")
  require(classOf != null, "undefined bean target class")
  require(scope != null, "undefined bean scope")
  require(dependencies != null, "undefined bean dependencies list")
}

final case class PrimitiveType(classOf: Class[_], value: AnyRef) {
  require(classOf != null, "undefined value target class")
  require(value != null, "undefined value")
}

final case class Dependency(either: Either[PrimitiveType, BeanRef], scope: InjectScope) {
  require(either != null, "undefined dependency type")
  require(scope != null, "undefined dependency scope")
}

final case class BeanRef(id: String) {
  require(id != null && !id.isEmpty, "undefined referenced bean id")
}

sealed trait InjectScope

case object Constructor extends InjectScope

object Setter

final case class Setter(field: String) extends InjectScope {
  require(field != null && !field.isEmpty, "undefined field name")
}

sealed trait BeanScope

object Singleton extends BeanScope

object PerInject extends BeanScope
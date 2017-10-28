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

/*final case class SharedProperty(id: String, override val classOf: Class[_ <: AnyRef], override val value: AnyRef) extends Property(classOf, value) {
  require(id != null && !id.isEmpty, "undefined bean id")
}*/

case class Property(classOf: Class[_], value: AnyRef) {
  require(classOf != null, "undefined value target class")
  require(value != null, "undefined value")
}

final case class Dependency(either: Either[Property, BeanRef], scope: InjectScope) {
  require(either != null, "undefined dependency type")
  require(scope != null, "undefined dependency scope")
}

final case class BeanRef(id: String) {
  require(id != null && !id.isEmpty, "undefined referenced bean id")
}

sealed trait InjectScope

case object Constructor extends InjectScope

final case class Field(field: java.lang.reflect.Field) extends InjectScope

object Setter

final case class Setter(method: java.lang.reflect.Method) extends InjectScope {
  require(method != null, "undefined method")
}

sealed trait BeanScope

object Singleton extends BeanScope

object PerInject extends BeanScope
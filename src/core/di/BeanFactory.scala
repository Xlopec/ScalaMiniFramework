package core.di

/**
  * Represents basic bean factory
  * contract
  * Created by Максим on 9/10/2017.
  */
trait BeanFactory {

  def instantiate[T](c: Class[T]): T

  def instantiate[T](id: String): T

}

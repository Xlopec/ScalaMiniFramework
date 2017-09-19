package core.di

/**
  * Represents basic bean context which
  * allows to access to a corresponding
  * bean factory implementation
  * Created by Максим on 9/10/2017.
  */
trait BeanContext {

  def getBeanFactory: BeanFactory

}

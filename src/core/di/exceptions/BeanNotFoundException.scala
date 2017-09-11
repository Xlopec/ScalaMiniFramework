package core.di.exceptions

/**
  * Created by Максим on 9/10/2017.
  */
class BeanNotFoundException(message: String, cause: Throwable) extends RuntimeException(message: String, cause: Throwable) {

  def this(message: String) = this(message, null)

}

package core.di.imp

object BeanUtil {
  /**
    * Creates bean identifier for a given class
    *
    * @param classOf bean class
    * @return string identifier for a given bean type
    */
  def createBeanId(classOf: Class[_]): String = classOf.getClass.getName

}

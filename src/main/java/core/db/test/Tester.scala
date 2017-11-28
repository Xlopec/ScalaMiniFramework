package core.db.test

import java.io.File

import core.db.{Dao, DaoManager}
import core.di.{BeanContext, BeanFactory}
import core.di.imp.XmlBeanContext

object Tester extends App {

  override def main(args: Array[String]): Unit = {
    val context: BeanContext = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/Db_Config.xml"))
    val factory: BeanFactory = context.getBeanFactory

    val daoManager: DaoManager = factory.instantiate(classOf[DaoManager])
    val daoStudent: Dao[Student, Integer] = daoManager.getDao(classOf[Student])
    val daoLesson: Dao[Lesson, Integer] = daoManager.getDao(classOf[Lesson])
    val supervisorDao: Dao[Supervisor, Lesson] = daoManager.getDao(classOf[Supervisor])

    val student = Student()

    student.firstName = "Max"
    student.lastName = "Oliynick"
    student.yearOfStudy = 5

    val supervisor = Supervisor()

    supervisor.firstName = "Ivan"
    supervisor.lastName = "Ivanov"
    student.supervisor = supervisor

    val studentId = daoStudent.create(student)

    student.id = studentId

    val lesson = Lesson()
    lesson.title = "Lesson1"
    lesson.description = "some lesson "
    lesson.student = student

    daoLesson.create(lesson)

    val fromDb = daoStudent.read(studentId)
    println(fromDb)
  }

}

package core.db.test

import java.util

import com.j256.ormlite.field.{DatabaseField => Column, ForeignCollectionField => ToMany}
import com.j256.ormlite.table.{DatabaseTable => Table}

object Student {
  def apply(): Student = new Student()
}

@Table(tableName = "Student")
final class Student private() {

  @Column(generatedId = true)
  var id: Int = _

  @Column(columnName = "first_name")
  var firstName: String = _

  @Column(columnName = "last_name")
  var lastName: String = _

  @Column(columnName = "year_of_study")
  var yearOfStudy: Int = _

  @Column(columnName = "supervisor",
    foreign = true,
    foreignAutoCreate = true,
    foreignAutoRefresh = true)
  var supervisor: Supervisor = _

  @ToMany(eager = true)
  var lessons: util.Collection[Lesson] = _

  override def toString: String =
    s"""id=$id,
       |firstName=$firstName lastName=$lastName,
       |year=$yearOfStudy, supervisor=$supervisor,
       |lessons=${new util.ArrayList[Lesson](lessons)}""".stripMargin
}

package core.db.test

import com.j256.ormlite.field.{DatabaseField => Column}
import com.j256.ormlite.table.{DatabaseTable => Table}

object Lesson {
  def apply(): Lesson = new Lesson()
}

@Table(tableName = "lesson")
final class Lesson private() {

  @Column(generatedId = true)
  var id: Int = _

  @Column(columnName = "title")
  var title: String = _

  @Column(columnName = "description")
  var description: String = _

  @Column(columnName = "user",
    foreign = true,
    foreignAutoRefresh = true)
  var student: Student = _

  override def toString: String = s"""id=$id, title=$title, description=$description, student=${student.id}"""

}

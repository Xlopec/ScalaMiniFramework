package core.db.test

import com.j256.ormlite.field.{DatabaseField => Column, ForeignCollectionField => ToMany}
import com.j256.ormlite.table.{DatabaseTable => Table}

object Supervisor {
  def apply(): Supervisor = new Supervisor()
}

@Table(tableName = "supervisor")
final class Supervisor private() {

  @Column(generatedId = true)
  var id: Long = _

  @Column(columnName = "first_name")
  var firstName: String = _

  @Column(columnName = "last_name")
  var lastName: String = _

  override def toString = s"""id=$id, firstName=$firstName, lastName=$lastName"""
}


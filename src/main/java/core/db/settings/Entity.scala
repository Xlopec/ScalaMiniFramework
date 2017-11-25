package core.db.settings

import java.lang.annotation.Annotation

import core.db.annotation
import core.db.annotation._

import scala.collection.mutable

object Entity {

  def apply(backedClass: Class[_ <: AnyRef]): Entity = {
    val collected = collectColumns(backedClass)

    Entity(backedClass, tableName(backedClass),
      backedClass.getAnnotation(classOf[annotation.Entity]).skipIfExists(),
      collected._1, collected._2, collected._3)
  }

  def tableName(e: Class[_]): String = {
    val a = e.getAnnotation(classOf[annotation.Entity])
    if (a.table() == null || a.table().isEmpty) e.getName.toUpperCase else a.table()
  }

  private def collectColumns(backedClass: Class[_ <: AnyRef]): (Key, Set[Column], Set[ForeignColumn]) = {

    def relationsAnnotationsCount(annotations: Option[Annotation]*) = annotations.foldLeft(0)((cnt, a) => if (a.isDefined) cnt + 1 else cnt)

    val columns = mutable.HashSet[Column]()
    val foreignColumns = mutable.HashSet[ForeignColumn]()
    var key: Key = null

    for (field <- backedClass.getDeclaredFields) {

      val id = Option(field.getAnnotation(classOf[Id]))
      val toOne = Option(field.getAnnotation(classOf[ToOne]))
      val toMany = Option(field.getAnnotation(classOf[ToMany]))
      val joinEntity = Option(field.getAnnotation(classOf[JoinEntity]))
      val count = relationsAnnotationsCount(toOne, toMany, joinEntity)

      require(count <= 1,
        s"""Annotation can be applied only once,
           |were $toOne, $toMany, $joinEntity""".stripMargin)

      require(id.isEmpty || (id.isDefined && count == 0),
        s"Cannot mix with other annotations with ${classOf[Id]}")

      if (id.isDefined) {
        require(key == null, s"Entity cannot have more than one ${classOf[Id]} annotation, found in $backedClass")
        key = Key(field)
      } else if (count == 1) {
        foreignColumns += ForeignColumn(field)
      } else {
        columns += Column(field)
      }
    }

    require(key != null, s"Not found key column for entity $backedClass")

    (key, columns.toSet, foreignColumns.toSet)
  }

}

final case class Entity private(backedClass: Class[_ <: AnyRef], name: String, skipIfExists: Boolean, key: Key, columns: Set[Column], foreignColumns: Set[ForeignColumn]) {

}


object Column {

  def apply(field: java.lang.reflect.Field): Column = {
    val annotation = field.getAnnotation(classOf[Property])

    require(annotation != null, s"Missing annotation ${classOf[Property]}")

    Column(field, createName(annotation).getOrElse(field.getName.toUpperCase), annotation.nullable(), createDefinition(annotation))
  }

  def createName(id: Property): Option[String] = {
    if (id.name() == null || id.name().isEmpty) {
      Option.empty
    } else {
      Option(id.name())
    }
  }

  def createDefinition(id: Property): Option[String] = {
    if (id.definition() == null || id.definition().isEmpty) {
      Option.empty
    } else {
      Option(id.definition())
    }
  }

}

object Key {

  def apply(field: java.lang.reflect.Field): Key = {
    val annotation = field.getAnnotation(classOf[Id])

    require(annotation != null, s"Missing annotation ${classOf[Id]}")

    Key(field, createName(annotation).getOrElse(field.getName.toUpperCase), annotation.autoincrement(),
      createDefinition(annotation))
  }

  def createName(id: Id): Option[String] = {
    if (id.name() == null || id.name().isEmpty) {
      Option.empty
    } else {
      Option(id.name())
    }
  }

  def createDefinition(id: Id): Option[String] = {
    if (id.definition() == null || id.definition().isEmpty) {
      Option.empty
    } else {
      Option(id.definition())
    }
  }
}

object ForeignColumn {

  def apply(field: java.lang.reflect.Field): ForeignColumn = {
    val toOne = field.getAnnotation(classOf[ToOne])

    require(toOne != null)

    ForeignColumn(field, field.getName.toUpperCase, toOne.joinColumn(), Entity.tableName(toOne.joinedEntity()), toOne.nullable())
  }

}

final case class Key private(field: java.lang.reflect.Field, name: String, autoIncrement: Boolean, definition: Option[String]) extends BaseColumn(field, name)

final case class Column private(field: java.lang.reflect.Field, name: String, nullable: Boolean, definition: Option[String]) extends BaseColumn(field, name)

final case class ForeignColumn private(field: java.lang.reflect.Field, name: String, foreignColumn: String, foreignTable: String, nullable: Boolean) extends BaseColumn(field, name) {
  require(foreignTable != null)
}

sealed abstract class BaseColumn protected(backedField: java.lang.reflect.Field, name: String) {
  require(backedField != null)
  require(name != null)

  val backedType: Class[_] = backedField.getType
}

/*
object BaseColumn {
  def createName(property: Property): Option[String] = {
    if (property.name() == null || property.name().isEmpty) {
      Option.empty
    } else {
      Option(property.name())
    }
  }

  def createDefinition(property: Property): Option[String] = {
    if (property.definition() == null || property.definition().isEmpty) {
      Option.empty
    } else {
      Option(property.definition())
    }
  }
}*/

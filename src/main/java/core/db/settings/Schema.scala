package core.db.settings

import java.lang.annotation.Annotation
import java.lang.reflect.Field

import core.db.annotation._
import core.db.{annotation, imp}

import scala.collection.mutable

object Schema {

  def apply(backedClass: Class[_ <: AnyRef]): Schema = {
    val collected = collectColumns(backedClass)

    Schema(backedClass, tableName(backedClass),
      backedClass.getAnnotation(classOf[annotation.Entity]).skipIfExists(),
      collected._1, collected._2, collected._3, collected._4, collected._5)
  }

  def tableName(e: Class[_]): String = {
    val a = e.getAnnotation(classOf[annotation.Entity])
    if (a.table() == null || a.table().isEmpty) e.getName.toUpperCase else a.table()
  }

  private def collectColumns(backedClass: Class[_ <: AnyRef]): (Key, Set[Column], Set[ToOneProperty], Set[ToManyProperty], Set[JoiningProperty]) = {

    def relationsAnnotationsCount(annotations: Option[Annotation]*) = annotations.foldLeft(0)((cnt, a) => if (a.isDefined) cnt + 1 else cnt)

    val columns = mutable.HashSet[Column]()
    val foreignColumns = mutable.HashSet[ToOneProperty]()
    val toManyColumns = mutable.HashSet[ToManyProperty]()
    val joiningColuns = mutable.HashSet[JoiningProperty]()
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

        if (toOne.isDefined) {
          foreignColumns += ToOneProperty(field)
        } else if (toMany.isDefined) {
          toManyColumns += ToManyProperty(field)
        } else {
          joiningColuns += JoiningProperty(field)
        }
      } else {
        columns += Column(field)
      }
    }

    require(key != null, s"Not found key column for entity $backedClass")

    (key, columns.toSet, foreignColumns.toSet, toManyColumns.toSet, joiningColuns.toSet)
  }

}

final case class Schema private(backedClass: Class[_ <: AnyRef], name: String, skipIfExists: Boolean, key: Key, columns: Set[Column],
                                toOneProps: Set[ToOneProperty], toManyProperty: Set[ToManyProperty], joiningProps: Set[JoiningProperty]) {

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

    Key(field, createName(field, annotation), annotation.autoincrement(), createDefinition(annotation))
  }

  def createName(field: Field, id: Id): String = {
    if (id.name() == null || id.name().isEmpty) {
      field.getName.toUpperCase
    } else {
      id.name()
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

object ToOneProperty {

  def apply(field: java.lang.reflect.Field): ToOneProperty = {
    val toOne = field.getAnnotation(classOf[ToOne])

    require(toOne != null)

    val joinEntity = toOne.joinedEntity()
    val keyAnnotation = imp.findFieldWithAnnotation(joinEntity, classOf[Id])

    require(keyAnnotation.isDefined, s"Missing ${classOf[Id]} annotation in entity $joinEntity")

    val referencedField = keyAnnotation.get._1

    ToOneProperty(field, field.getName.toUpperCase, Key.createName(referencedField, keyAnnotation.get._2),
      Schema.tableName(joinEntity), toOne.nullable(), field.getType.asInstanceOf[Class[_ <: AnyRef]],
      referencedField.getType)
  }

}

object ToManyProperty {

  def apply(field: java.lang.reflect.Field): ToManyProperty = {
    val toMany = field.getAnnotation(classOf[ToMany])

    require(toMany != null)

    val joinEntity = toMany.joinedEntity()
    val keyAnnotation = imp.findFieldWithAnnotation(joinEntity, classOf[Id])

    require(keyAnnotation.isDefined, s"Missing ${classOf[ToMany]} annotation in entity $joinEntity")

    val referencedField = keyAnnotation.get._1

    /*val fieldType = field.getType match {
      case c: Class[java.util.List[_]] => {
        val gg = c.getGenericInterfaces()(0)

        val pp = gg.asInstanceOf[ParameterizedType].getActualTypeArguments()(0)

        val tt = pp.getTypeName

        val cc = pp.getClass

        cc
      }
      case t => t
    }*/

    ToManyProperty(field, field.getName.toUpperCase, Key.createName(referencedField, keyAnnotation.get._2),
      Schema.tableName(joinEntity), toMany.joinedEntity().asInstanceOf[Class[_ <: AnyRef]], referencedField.getType)
  }

}

object JoiningProperty {

  def apply(field: java.lang.reflect.Field): JoiningProperty = {
    val joinOn = field.getAnnotation(classOf[JoinEntity])

    require(joinOn != null, s"Missing ${classOf[JoinEntity]} annotation")

    JoiningProperty(field, field.getName.toUpperCase, joinOn.sourceProperty(), joinOn.targetProperty(),
      field.getType.asInstanceOf[Class[_ <: AnyRef]], joinOn.joiningEntity().asInstanceOf[Class[_ <: AnyRef]])
  }

}

final case class Key private(field: java.lang.reflect.Field, name: String, autoIncrement: Boolean, definition: Option[String]) extends BaseColumn(field, name)

final case class Column private(field: java.lang.reflect.Field, name: String, nullable: Boolean, definition: Option[String]) extends BaseColumn(field, name)

/**
  * Foreign column representation
  *
  * @param field                this column backed field
  * @param name                 this column name
  * @param foreignColumn        referenced column name in a table
  * @param foreignTable         referenced table name
  * @param nullable             if relation is optional
  * @param backedType           this column type
  * @param backedReferencedType referenced column type
  */
final case class ToOneProperty private(field: java.lang.reflect.Field, name: String, foreignColumn: String, foreignTable: String,
                                       nullable: Boolean, override val backedType: Class[_ <: AnyRef], backedReferencedType: Class[_]) extends BaseColumn(field, name) {
  require(foreignTable != null)
  require(backedReferencedType != null)
}

final case class ToManyProperty private(field: java.lang.reflect.Field, name: String, foreignColumn: String, foreignTable: String,
                                        override val backedType: Class[_ <: AnyRef], backedReferencedType: Class[_]) extends BaseColumn(field, name) {
  require(foreignTable != null)
  require(backedReferencedType != null)
}

final case class JoiningProperty private(field: java.lang.reflect.Field, name: String, sourceProperty: String, targetProperty: String,
                                         override val backedType: Class[_ <: AnyRef], backedTableType: Class[_ <: AnyRef]) extends BaseColumn(field, name) {
}

sealed abstract class BaseColumn protected(backedField: java.lang.reflect.Field, name: String) {
  require(backedField != null)
  require(name != null)

  val backedType: Class[_] = backedField.getType
}
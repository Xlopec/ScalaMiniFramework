package core.db

import java.lang
import java.lang.annotation.Annotation
import java.lang.reflect.Field

import core.db.settings.{Column, ToOneProperty, Key}

package object imp {

  private val sqlMapping: Map[String, String] = Map(classOf[lang.String].getName -> "TEXT", classOf[lang.Integer].getName -> "INT",
    classOf[lang.Byte].getName -> "TINYINT", classOf[lang.Boolean].getName -> "BOOL", classOf[lang.Float].getName -> "FLOAT",
    classOf[lang.Double].getName -> "DOUBLE", classOf[lang.Long].getName -> "BIGINT",
    "long" -> "BIGINT", "int" -> "INT", "String" -> "TEXT", "double" -> "DOUBLE", "byte" -> "TINYINT",
    "boolean" -> "BOOL")

  def findAnnotatedField(cl: Class[_], annotation: Class[_ <: Annotation]): Option[Field] = {
    val fields = cl.getDeclaredFields

    def lookup(i: Int): Option[Field] = {
      if (i + 1 >= fields.length) Option.empty
      else if (fields(i).getAnnotation(annotation) != null) Option(fields(i))
      else lookup(i + 1)
    }

    lookup(0)
  }

  def findFieldWithAnnotation[A <: Annotation](cl: Class[_], annotation: Class[A]): Option[(Field, A)] = {
    val fields = cl.getDeclaredFields

    def lookup(i: Int): Option[(Field, A)] = {
      if (i + 1 >= fields.length) Option.empty
      else if (fields(i).getAnnotation(annotation) != null) Option((fields(i), fields(i).getAnnotation(annotation)))
      else lookup(i + 1)
    }

    lookup(0)
  }

  private[imp] def typeToSqlType(name: String) = sqlMapping.get(name)

  private[imp] def typeToSqlType(cl: Class[_]) = sqlMapping(cl.getName)

  private[imp] def createTableDefinition(entity: settings.Schema): String = {
    require(entity != null)

    val sql = s"CREATE TABLE ${if (entity.skipIfExists) "IF NOT EXISTS" else ""} " +
      s"`${entity.name}` ${createColumnsDefinition(entity)}"

    sql
  }

  private[imp] def createColumnsDefinition(entity: settings.Schema): String = {
    val sql = appendId(entity.key)

    appendColumns(sql, entity.columns)
    appendForeignColumns(sql, entity.toOneProps)

    sql.setLength(sql.length - 1)
    sql.append(")").toString()
  }

  private def appendId(key: Key) = {
    val sql = new StringBuilder("(")

    if (key.definition.isDefined) {
      sql.append(key.definition)
    } else {
      sql.append('`').append(key.name).append("` ").append(typeToSqlType(key.backedType))
        .append(s" PRIMARY KEY NOT NULL ${if (key.autoIncrement) "AUTO_INCREMENT" else ""},")
    }
  }

  private def appendColumns(sql: StringBuilder, columns: Iterable[Column]) = {
    for (column <- columns) {

      if (column.definition.isDefined) {
        sql.append(column.definition)
      } else {
        appendColumnDefinition(sql, column.name, column.backedType, column.nullable)
      }

      sql.append(",")
    }

    sql
  }

  private def appendForeignColumns(sql: StringBuilder, columns: Iterable[ToOneProperty]) = {
    for (foreign <- columns) {
      appendColumnDefinition(sql, foreign.name, foreign.backedReferencedType, foreign.nullable)
        .append(", FOREIGN KEY (`").append(foreign.name).append("`) REFERENCES ")
        .append(foreign.foreignTable).append("(`").append(foreign.foreignColumn).append("`),")
    }

    sql
  }

  private def appendColumnDefinition(sql: StringBuilder, name: String, backedType: Class[_], isNullable: Boolean) = {
    sql.append('`').append(name).append("` ").append(typeToSqlType(backedType))
      .append(s" ${if (isNullable) "" else " NOT NULL"}")
  }

}

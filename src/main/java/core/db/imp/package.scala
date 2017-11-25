package core.db

import java.lang

import core.db.settings.{Column, ForeignColumn, Key}

package object imp {

  private val sqlMapping: Map[String, String] = Map(classOf[lang.String].getName -> "TEXT", classOf[lang.Integer].getName -> "INT",
    classOf[lang.Byte].getName -> "TINYINT", classOf[lang.Boolean].getName -> "BOOL", classOf[lang.Float].getName -> "FLOAT",
    classOf[lang.Double].getName -> "DOUBLE", classOf[lang.Long].getName -> "BIGINT",
    "long" -> "BIGINT", "int" -> "INT", "String" -> "TEXT", "double" -> "DOUBLE", "byte" -> "TINYINT",
    "boolean" -> "BOOL")

  private[imp] def typeToSqlType(name: String) = sqlMapping.get(name)

  private[imp] def typeToSqlType(cl: Class[_]) = sqlMapping(cl.getName)

  private[imp] def createTableDefinition(entity: settings.Entity): String = {
    require(entity != null)

    val sql = s"CREATE TABLE ${if (entity.skipIfExists) "IF NOT EXISTS" else ""} " +
      s"`${entity.name}` ${createColumnsDefinition(entity)}"

    sql
  }

  private[imp] def createColumnsDefinition(entity: settings.Entity): String = {
    val sql = appendId(entity.key)

    appendColumns(sql, entity.columns)
    appendForeignColumns(sql, entity.foreignColumns)

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
        sql.append('`').append(column.name).append("` ").append(typeToSqlType(column.backedType))
          .append(s" ${if (column.nullable) "" else " NOT NULL"}")
      }

      sql.append(",")
    }

    sql
  }

  private def appendForeignColumns(sql: StringBuilder, columns: Iterable[ForeignColumn]) = {
    for (foreign <- columns) {
      sql.append("FOREIGN KEY (").append(foreign.name).append(") REFERENCES ")
        .append(foreign.foreignTable).append("(").append(foreign.foreignColumn).append("),")
    }

    sql
  }

}

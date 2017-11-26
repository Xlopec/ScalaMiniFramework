package core.db

import java.lang
import java.lang.annotation.Annotation
import java.lang.reflect.Field

import core.db.settings._

import scala.collection.mutable

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

  def findFieldWithAnnotationExact[A <: Annotation](cl: Class[_], annotation: Class[A]): Option[(Field, A)] = {
    val fields = cl.getDeclaredFields

    def lookup(i: Int): Option[(Field, A)] = {
      if (i >= fields.length) Option.empty
      else if (fields(i).getAnnotation(annotation) != null) Option((fields(i), fields(i).getAnnotation(annotation)))
      else lookup(i + 1)
    }

    lookup(0)
  }

  def findFieldWithAnnotation[A <: Annotation](cl: Class[_], annotation: Class[A]): Set[Field] = {
    val fields = cl.getDeclaredFields

    def lookup(i: Int, set: mutable.Set[Field]): mutable.Set[Field] = {
      if (i >= fields.length) set
      else if (fields(i).getAnnotation(annotation) != null) {
        set += fields(i)
        lookup(i + 1, set)
      }
      else lookup(i + 1, set)
    }

    lookup(0, new mutable.HashSet[Field]()).toSet
  }

  private[imp] def typeToSqlType(name: String) = sqlMapping.get(name)

  private[imp] def typeToSqlType(cl: Class[_]) = sqlMapping(cl.getName)

  private[imp] def createSelectQuery(schema: settings.Schema): (Map[BaseColumn, String], String) = {
    require(schema != null)

    val sql = new StringBuilder("SELECT ")
    val aliasToTable = createAliasMapping(schema, "t")

    for (alias <- aliasToTable.values) {
      sql.append(alias).append(".*,")
    }

    sql.setLength(sql.length - 1)
    sql.append("\n FROM ").append(schema.name).append(' ')
      .append(aliasToTable(schema.key)).append(' ')

    var offset = 1
    // appends sql join statement
    val joiner = (column: BaseColumn, alias: String) => {
      column match {
        case toOne: ToOneProperty => Option(createToOneStatement(toOne, alias, aliasToTable(schema.key)))
        case toMany: ToManyProperty => Option(createToManyStatement(toMany, alias, aliasToTable(schema.key), schema.key.name))
        case joinTable: JoiningProperty =>
          val joinAlias = createAlias("t", aliasToTable.size + offset)

          offset += 1

          aliasToTable.put(joinTable.sourceToOne, joinAlias)

          Option(createManyToManyStatement(joinTable, alias, aliasToTable(schema.key), schema.key.name, joinAlias))
        case _ => Option.empty
      }
    }

    for ((column, alias) <- aliasToTable) {

      val join = joiner(column, alias)

      if (join.isDefined) {
        sql.append(join.get).append("\n")
      }
    }

    (aliasToTable.toMap, sql.toString)
  }

  private def createToOneStatement(toOne: ToOneProperty, leftAlias: String, rightAlias: String) = s"INNER JOIN ${toOne.foreignTable} $leftAlias ON $leftAlias.${toOne.foreignColumn} = $rightAlias.${toOne.name}"

  private def createToManyStatement(toMany: ToManyProperty, leftAlias: String, rightAlias: String, rightColumn: String) = s"INNER JOIN ${toMany.foreignTable} $leftAlias ON $leftAlias.${toMany.foreignColumn} = $rightAlias.$rightColumn"

  private def createManyToManyStatement(joinTable: JoiningProperty, leftAlias: String, rightAlias: String, rightColumn: String, joinAlias: String) =
    s"""INNER JOIN ${joinTable.table} $leftAlias ON $leftAlias.${joinTable.targetToOne.name} = $rightAlias.$rightColumn
       |INNER JOIN ${joinTable.sourceToOne.foreignTable} $joinAlias ON $joinAlias.${joinTable.sourceToOne.foreignColumn} = $leftAlias.${joinTable.sourceToOne.name}
           """.stripMargin

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

  private def createAliasMapping(schema: settings.Schema, alias: String): mutable.Map[BaseColumn, String] = {
    val aliasToTable = new mutable.HashMap[BaseColumn, String]()
    var index = 0

    def createAlias(): String = {
      index += 1
      s"$alias$index"
    }

    aliasToTable.put(schema.key, createAlias())

    for (toOne <- schema.toOneProps) {
      aliasToTable.put(toOne, createAlias())
    }

    for (toMany <- schema.toManyProperty) {
      aliasToTable.put(toMany, createAlias())
    }

    for (joinTable <- schema.joiningProps) {
      aliasToTable.put(joinTable, createAlias())
    }

    aliasToTable
  }

  private def createAlias(alias: String, i: Int) = s"$alias$i"

}

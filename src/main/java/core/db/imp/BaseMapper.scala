package core.db.imp

import java.lang.reflect.Field
import java.sql.ResultSet
import java.util

import core.db.Mapper
import core.db.settings.{BaseColumn, Column, Schema, ToManyProperty}

class BaseMapper[E <: AnyRef] extends Mapper[E] {

  override def map(schema: Schema, result: ResultSet, aliases: Map[BaseColumn, String]): E = {
    val entity = schema.backedClass.newInstance().asInstanceOf[E]

    while (result.next()) {

      setField(entity, schema.key.field, result.getObject(schema.key.name))
      setColumns(entity, schema.columns, result, aliases)
      setToManyColumns(entity, schema.toManyProperty, result, aliases)

    }

    entity
  }

  private def setColumns(on: AnyRef, columns: Iterable[Column], result: ResultSet, aliases: Map[BaseColumn, String]): Unit = {
    for (col <- columns) {
      setField(on, col.field, result.getObject(aliases(col), col.backedType))
    }
  }

  private def setToManyColumns(on: AnyRef, toManyProps: Iterable[ToManyProperty], result: ResultSet, aliases: Map[BaseColumn, String]): Unit = {
    val list = new util.ArrayList[Any]()

    for (col <- toManyProps) {
      list.add(result.getObject(aliases(col), col.backedType))
    }
  }

  private def setField(on: AnyRef, field: Field, value: Any): Unit = {
    val mAccessible = field.isAccessible

    if (!mAccessible) {
      field.setAccessible(true)
    }

    field.set(on, value)

    if (!mAccessible) {
      field.setAccessible(false)
    }
  }

}

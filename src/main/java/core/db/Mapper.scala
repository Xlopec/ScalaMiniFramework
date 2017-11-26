package core.db

import java.sql.ResultSet

import core.db.settings.{BaseColumn, Schema}

trait Mapper[E <: AnyRef] {

  def map(schema: Schema, result: ResultSet, aliases: Map[BaseColumn, String]): E

}

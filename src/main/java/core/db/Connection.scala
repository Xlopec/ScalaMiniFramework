package core.db

import java.sql.ResultSet

trait Connection {

  def execSql(sql: String) : Unit

  def query(sql: String) : ResultSet

  def connect() : Unit

  def disconnect() : Unit

}

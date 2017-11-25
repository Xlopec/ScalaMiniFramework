package core.db

trait Connection {

  def execSql(sql: String) : Unit

  def connect() : Unit

  def disconnect() : Unit

}

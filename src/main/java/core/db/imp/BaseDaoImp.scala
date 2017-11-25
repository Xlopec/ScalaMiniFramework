package core.db.imp

import core.db.settings.Entity
import core.db.{Connection, Dao}

class BaseDaoImp[E <: AnyRef, K](val connection: Connection, val entity: Entity) extends Dao[E, K] {

  override def create(entity: E): K = ???

  override def read(key: K): E = ???

  override def update(entity: E): Unit = ???

  override def delete(key: K): Unit = ???

  override protected[db] def createTable(): Unit = {
    val sql = createTableDefinition(entity)

    connection.execSql(sql)
  }
}

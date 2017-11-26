package core.db.imp

import core.db.settings.Schema
import core.db.{Connection, Dao, DaoManager}

class BaseDaoImp[E <: AnyRef, K](private val connection: Connection, val schema: Schema, protected val daoManager: DaoManager) extends Dao[E, K] {

  protected val mapper = new BaseMapper

  override def create(entity: E): K = ???

  override def read(key: K): E = {
    val mappedQuery = createSelectQuery(schema)

    val query = connection.query(mappedQuery._2)

    mapper.map(schema, query, mappedQuery._1)
  }

  override def update(entity: E): Unit = ???

  override def delete(key: K): Unit = ???

  override protected[db] def createTable(): Unit = {
    val sql = createTableDefinition(schema)

    connection.execSql(sql)
  }
}

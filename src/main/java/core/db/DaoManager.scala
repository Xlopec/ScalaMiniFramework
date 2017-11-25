package core.db

import core.db.imp.BaseDaoImp
import core.db.settings.Entity
import core.di.annotation.Component

import scala.collection.mutable

@Component
final class DaoManager(connection: Connection) {
  connection.connect()

  private val map = new mutable.HashMap[Class[_ <: AnyRef], Dao[_, _]]

  def getDao[E <: AnyRef, K](cl: Class[E]): Dao[E, K] = {
    require(cl != null)

    //synchronized(map) {
      map.getOrElseUpdate(cl, createDao[E, K](cl)).asInstanceOf[Dao[E, K]]
    //}
  }

  def release(): Unit = {
    map.clear()
    connection.disconnect()
  }

  private def createDao[E <: AnyRef, K](cl: Class[E]): Dao[E, K] = {
    val entity = Entity(cl)

    if (entity.foreignColumns.nonEmpty) {
      // long path, create tables without references
      // to other tables
      // TODO: add cyclic dependencies check
      getDao(cl).createTable()
    }

    val dao = new BaseDaoImp[E, K](connection, entity)

    dao.createTable()
    dao
  }

}
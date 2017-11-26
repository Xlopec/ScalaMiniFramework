package core.db

import core.db.imp.BaseDaoImp
import core.db.settings.Schema
import core.di.annotation.Component

import scala.collection.mutable

@Component
final class DaoManager(connection: Connection) {
  connection.connect()

  private val map = new mutable.HashMap[Class[_ <: AnyRef], Dao[_, _]]

  def getDao[E <: AnyRef, K](cl: Class[E]): Dao[E, K] = {
    require(cl != null)

    getDao(cl, new mutable.HashSet[Class[_]]())
  }

  def release(): Unit = {
    map.clear()
    connection.disconnect()
  }

  private def getDao[E <: AnyRef, K](cl: Class[E], createChain: mutable.Set[Class[_]]): Dao[E, K] = {
    map.synchronized {

      val cachedDao = map.get(cl)

      if (cachedDao.isDefined) {
        cachedDao.asInstanceOf[Dao[E, K]]
      } else {
        createDao[E, K](cl, createChain)
        map(cl).asInstanceOf[Dao[E, K]]
      }
    }
  }

  private def createDao[E <: AnyRef, K](cl: Class[E], createChain: mutable.Set[Class[_]]): Unit = {
    if (createChain.contains(cl)) {
      return
    }

    createChain += cl

    val entity = Schema(cl)
    // long path, create tables without references
    // to other tables
    for (foreign <- entity.toOneProps if !hasCachedDao(foreign.backedType)) {
      createDao(foreign.backedType, createChain)
      createChain += foreign.backedType
    }

    val dao = new BaseDaoImp[E, K](connection, entity, this)

    dao.createTable()

    for (foreign <- entity.toManyProperty if !hasCachedDao(foreign.backedType)) {
      createDao(foreign.backedType, createChain)
      createChain += foreign.backedType
    }

    for (joining <- entity.joiningProps if !hasCachedDao(joining.backedTableType)) {
      createDao(joining.backedTableType, createChain)
      createChain += joining.backedTableType
    }

    map.put(cl, dao)
  }

  private def hasCachedDao(cl: Class[_ <: AnyRef]) = map.get(cl).isDefined

}
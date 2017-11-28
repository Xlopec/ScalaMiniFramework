package core.db

import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import core.db.exception.DbException
import core.db.imp.BaseDaoImp
import core.db.settings.ConnectionSettings
import core.di.annotation.Component

import scala.collection.mutable

@Component
final class DaoManager(settings: ConnectionSettings) {

  private val connection = connect()

  private val map = new mutable.HashMap[Class[_ <: AnyRef], Dao[_, _]]

  def getDao[E <: AnyRef, K](cl: Class[E]): Dao[E, K] = {
    require(cl != null)

    map.synchronized {
      map.getOrElseUpdate(cl, createDao(cl)).asInstanceOf[Dao[E, K]]
    }
  }

  def release(): Unit = {
    map.clear()
    connection.close()
  }

  private def connect(): ConnectionSource = {
    try {
      Class.forName(settings.driver)

      if (settings.user != null && settings.password != null) {
        new JdbcConnectionSource(settings.host, settings.user, settings.password)
      } else {
        new JdbcConnectionSource(settings.host)
      }
    } catch {
      case e: Exception => throw new DbException(s"Couldn't access db $settings", e)
    }
  }

  private def createDao[E <: AnyRef, K](value: Class[E]): BaseDaoImp[E, K] = {
    TableUtils.createTableIfNotExists(connection, value)
    new BaseDaoImp(DaoManager.createDao(connection, value)).asInstanceOf[BaseDaoImp[E, K]]
  }

}
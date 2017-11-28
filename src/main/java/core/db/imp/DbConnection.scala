package core.db.imp

import java.sql.ResultSet

import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.support.ConnectionSource
import core.db.Connection
import core.db.exception.DbException
import core.db.settings.ConnectionSettings
import core.di.annotation.Component

@Component
final class DbConnection(settings: ConnectionSettings) extends Connection {
  require(settings != null)

  var connection: ConnectionSource = _

  override def execSql(sql: String): Unit = {
    require(connection != null)
    log(sql)
  }

  override def connect(): Unit = {
    try {
      Class.forName(settings.driver)

      if (settings.user != null && settings.password != null) {
        connection = new JdbcConnectionSource(settings.host, settings.user, settings.password)
      } else {
        connection = new JdbcConnectionSource(settings.host)
      }
    } catch {
      case e: Exception => throw new DbException(s"Couldn't access db $settings", e)
    }
  }

  override def disconnect(): Unit = {
    if (connection != null) {
      connection.close()
    }
  }

  override def query(sql: String): ResultSet = {
    require(connection != null)
    log(sql)
    null
  }

  private def log(sql: String): Unit = {
    if (settings.verbose) {
      println(s"exec sql: $sql")
    }
  }

}

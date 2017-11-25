package core.db.imp

import java.sql
import java.sql.DriverManager

import core.db.Connection
import core.db.exception.DbException
import core.db.settings.ConnectionSettings
import core.di.annotation.Component

@Component
final class JdbcConnection(settings: ConnectionSettings) extends Connection {
  require(settings != null)

  var connection: sql.Connection = _

  override def execSql(sql: String): Unit = {
    require(connection != null)
    connection.createStatement().execute(sql)
  }

  override def connect(): Unit = {
    try {
      Class.forName(settings.driver)

      if (settings.user != null && settings.password != null) {
        connection = DriverManager.getConnection(settings.host, settings.user, settings.password)
      } else {
        connection = DriverManager.getConnection(settings.host)
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
}

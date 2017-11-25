package core.db.exception

class DbException(message: String = null, th: Throwable = null) extends RuntimeException(message, th)

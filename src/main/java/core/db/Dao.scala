package core.db

trait Dao[E, K] {

  protected[db] def createTable()

  def create(entity: E): K

  def read(key: K): E

  def update(entity: E): Unit

  def delete(key: K): Unit

}

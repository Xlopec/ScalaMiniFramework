package core.db

trait Dao[E, K] {

  def create(entity: E): K

  def read(key: K): E

  def update(entity: E): Unit

  def delete(key: K): Unit

}

package core.db.imp

class BaseDaoImp[E <: AnyRef, K](private val dao: com.j256.ormlite.dao.Dao[E, K]) extends core.db.Dao[E, K] {

  override def create(entity: E): K = dao.create(entity).asInstanceOf[K]

  override def read(key: K): E = dao.queryForId(key)

  override def update(entity: E): Unit = dao.update(entity)

  override def delete(key: K): Unit = dao.deleteById(key)
}

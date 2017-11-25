package core.db.settings

import core.di.annotation.{Autowiring, Component}

@Component
final case class ConnectionSettings(@Autowiring(named = "host") host: String,
                                    @Autowiring(named = "driver") driver: String,
                                    @Autowiring(named = "user") user: String = null,
                                    @Autowiring(named = "password") password: String = null) {
  require(host != null && !host.isEmpty)
  require(driver != null && !driver.isEmpty)
}

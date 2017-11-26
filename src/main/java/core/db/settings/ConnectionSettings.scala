package core.db.settings

import core.di.annotation.{Autowiring, Component}

@Component
final case class ConnectionSettings(@Autowiring(named = "host") host: String,
                                    @Autowiring(named = "driver") driver: String,
                                    @Autowiring(named = "user") user: String = null,
                                    @Autowiring(named = "password") password: String = null,
                                    @Autowiring(named = "verbose") verbose: Boolean = true) {
  require(host != null && !host.isEmpty)
  require(driver != null && !driver.isEmpty)
}

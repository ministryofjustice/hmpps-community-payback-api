package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)

  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresqlContainer() }
  private fun startPostgresqlContainer(): PostgreSQLContainer<Nothing>? = if (checkPostgresRunning().not()) {
    PostgreSQLContainer<Nothing>("postgres:16.8").apply {
      log.info("Starting postgres via test containers")
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withExposedPorts(5432)
      withDatabaseName("community_payback")
      withUsername("community_payback")
      withPassword("community_payback")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)
      start()
    }
  } else {
    log.info("Postgres already available, won't start via test containers")
    null
  }

  private fun checkPostgresRunning(): Boolean = try {
    val serverSocket = ServerSocket(5432)
    serverSocket.localPort == 0
  } catch (_: IOException) {
    true
  }

  fun setPostgresProperties(postgreSQLContainer: PostgreSQLContainer<Nothing>, registry: DynamicPropertyRegistry) {
    registry.add("spring.datasource.url") { postgreSQLContainer.jdbcUrl }
    registry.add("spring.datasource.username") { postgreSQLContainer.username }
    registry.add("spring.datasource.password") { postgreSQLContainer.password }
  }
}

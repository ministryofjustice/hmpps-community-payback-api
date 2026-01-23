package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

object RedisContainer {
  private val log = LoggerFactory.getLogger(this::class.java)

  val instance: com.redis.testcontainers.RedisContainer? by lazy { startRedisContainer() }

  private fun startRedisContainer() = if (checkRedisAlreadyRunning().not()) {
    log.info("Creating a Redis container")
    com.redis.testcontainers.RedisContainer(DockerImageName.parse("redis:7.0.4")).apply {
      withEnv("ALLOW_EMPTY_PASSWORD", "yes")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)
      start()
    }
  } else {
    log.info("Redis already available, won't start via test containers")
    null
  }

  private fun checkRedisAlreadyRunning(): Boolean = try {
    val serverSocket = ServerSocket(6379)
    serverSocket.localPort == 0
  } catch (_: IOException) {
    true
  }

  fun setRedisProperties(container: com.redis.testcontainers.RedisContainer, registry: DynamicPropertyRegistry) {
    registry.add("spring.data.redis.host") { "localhost" }
    registry.add("spring.data.redis.port") { container.redisPort }
  }
}

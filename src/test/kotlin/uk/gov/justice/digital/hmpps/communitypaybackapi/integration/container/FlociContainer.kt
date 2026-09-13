package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket
import kotlin.getValue

object FlociContainer {
  private val log = LoggerFactory.getLogger(this::class.java)

  val instance by lazy { startFlociIfNotRunning() }

  fun setFlociProperties(flociContainer: io.floci.testcontainers.FlociContainer, registry: DynamicPropertyRegistry) {
    val snsUrl = flociContainer.endpoint
    val region = flociContainer.region
    registry.add("hmpps.sqs.localstackUrl") { snsUrl }
    registry.add("hmpps.sqs.region") { region }
  }

  private fun startFlociIfNotRunning(): io.floci.testcontainers.FlociContainer? {
    if (flociIsRunning()) {
      log.info("Floci already available, won't start via test containers")
      return null
    }
    log.info("Starting Floci via test containers")
    val logConsumer = Slf4jLogConsumer(log).withPrefix("floci")
    return io.floci.testcontainers.FlociContainer().apply {
      withEnv("FLOCI_DEFAULT_REGION", "eu-west-2")
      waitingFor(
        Wait.forLogMessage(".*=== AWS Local Emulator Ready ===.*", 1),
      )
      start()
      followOutput(logConsumer)
    }
  }

  private fun flociIsRunning(): Boolean = try {
    val serverSocket = ServerSocket(4566)
    serverSocket.localPort == 0
  } catch (_: IOException) {
    true
  }
}

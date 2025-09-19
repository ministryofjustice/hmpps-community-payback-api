package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

/**
 * Manages a LocalStack instance for integration tests using Test Containers
 *
 * If a running instance is found, a new instance won't be started. This is required
 * when running in github actions, where local stack is managed by gradle. This also
 * means that when using cp-stack, the cp-stack managed instance will be used instead
 */
object LocalStackContainer {
  private val log = LoggerFactory.getLogger(this::class.java)

  val instance by lazy { startLocalStackIfNotRunning() }

  fun setLocalStackProperties(localStackContainer: LocalStackContainer, registry: DynamicPropertyRegistry) {
    val localstackSnsUrl = localStackContainer.getEndpointOverride(LocalStackContainer.Service.SNS).toString()
    val region = localStackContainer.region
    registry.add("hmpps.sqs.localstackUrl") { localstackSnsUrl }
    registry.add("hmpps.sqs.region") { region }
  }

  private fun startLocalStackIfNotRunning(): LocalStackContainer? {
    if (localstackIsRunning()) {
      log.info("Localstack already available, won't start via test containers")
      return null
    }
    log.info("Starting localstack via test containers")
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    return LocalStackContainer(
      DockerImageName.parse("localstack/localstack"),
    ).apply {
      withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS)
      withEnv("DEFAULT_REGION", "eu-west-2")
      waitingFor(
        Wait.forLogMessage(".*Ready.\n", 1),
      )
      start()
      followOutput(logConsumer)
    }
  }

  private fun localstackIsRunning(): Boolean = try {
    val serverSocket = ServerSocket(4566)
    serverSocket.localPort == 0
  } catch (_: IOException) {
    true
  }
}

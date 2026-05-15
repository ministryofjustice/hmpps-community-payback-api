package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.TelemetryService

@Service
@Primary
class MockTelemetryService : TelemetryService {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  private val capturedEvents = mutableListOf<CapturedEvent>()

  data class CapturedEvent(
    val name: String,
    val properties: Map<String, String?>,
    val metrics: Map<String, Double?>,
  )

  override fun trackEvent(
    name: String,
    properties: Map<String, String?>,
    metrics: Map<String, Double?>,
  ) {
    log.info("Telemetry event received with name $name")
    capturedEvents.add(CapturedEvent(name, properties, metrics))
  }

  fun hasEventsWithName(expectedName: String) = capturedEvents.any { it.name == expectedName }

  fun getEventsWithName(name: String) = capturedEvents.filter { it.name == name }

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedEvents.clear()
  }
}

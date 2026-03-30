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

  private val capturedEvents = mutableListOf<String>()

  override fun trackEvent(
    name: String,
    properties: Map<String, String?>,
    metrics: Map<String, Double?>,
  ) {
    log.info("Telemetry event received with name $name")
    capturedEvents.add(name)
  }

  fun hasEventsWithName(expectedName: String) = capturedEvents.contains(expectedName)

  @BeforeTestMethod
  fun beforeTestMethod() {
    reset()
  }

  private fun reset() {
    capturedEvents.clear()
  }
}

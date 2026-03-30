package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.util.UriUtils
import java.nio.charset.Charset

interface TelemetryService {
  fun trackEvent(
    name: String,
    properties: Map<String, String?> = mapOf(),
    metrics: Map<String, Double?> = mapOf(),
  )
}

@Service
class AppInsightsTelemetryService(private val telemetryClient: TelemetryClient = TelemetryClient()) : TelemetryService {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async
  override fun trackEvent(
    name: String,
    properties: Map<String, String?>,
    metrics: Map<String, Double?>,
  ) {
    log.debug(
      "{} {} {}",
      UriUtils.encode(name, Charset.defaultCharset()),
      UriUtils.encode(properties.toString(), Charset.defaultCharset()),
      UriUtils.encode(metrics.toString(), Charset.defaultCharset()),
    )
    telemetryClient.trackEvent(
      name,
      properties.filterValues { it != null },
      metrics.filterValues { it != null },
    )
  }
}

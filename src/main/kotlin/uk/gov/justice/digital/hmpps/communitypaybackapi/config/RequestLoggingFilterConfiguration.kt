package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EnvironmentService

@Configuration
@ConditionalOnProperty(name = ["community-payback.request-logging-enabled"], havingValue = "true")
class RequestLoggingFilterConfiguration(
  val environmentService: EnvironmentService,
  @param:Value("\${logging.request.include-headers:true}") private val includeHeaders: Boolean,
) {
  @Bean
  fun logFilter(): CommonsRequestLoggingFilter? {
    if (environmentService.isNotATestEnvironment()) {
      error("request logging should not be enabled outside of test environments")
    }
    val filter = CommunityPaybackRequestLoggingFilter()
    filter.setIncludeQueryString(true)
    filter.setIncludePayload(true)
    filter.setMaxPayloadLength(10000)
    filter.setIncludeHeaders(includeHeaders)
    filter.setAfterMessagePrefix("Request data: ")
    return filter
  }

  class CommunityPaybackRequestLoggingFilter : CommonsRequestLoggingFilter() {
    override fun shouldLog(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/health")
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import jakarta.annotation.PostConstruct
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EnvironmentService
import java.io.IOException

@Component
@ConditionalOnProperty(name = ["community-payback.request-logging-enabled"], havingValue = "true")
class RequestLoggingFilter(
  val environmentService: EnvironmentService,
) : OncePerRequestFilter() {

  @PostConstruct
  fun checkEnvironment() {
    if (environmentService.isNotATestEnvironment()) {
      error("request logging should not be enabled outside of test environments")
    }
  }

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    if (request.requestURI.startsWith("/health")) {
      filterChain.doFilter(request, response)
    } else {
      applyFilterWithLogging(request, response, filterChain)
    }
  }

  private fun applyFilterWithLogging(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val wrappedRequest = if (request is ContentCachingRequestWrapper) {
      request
    } else {
      ContentCachingRequestWrapper(request, 0)
    }
    val wrappedResponse = ContentCachingResponseWrapper(response)

    filterChain.doFilter(wrappedRequest, wrappedResponse)

    val requestBody = wrappedRequest.requestBodyString()
    val responseBody = wrappedResponse.responseBodyString()

    val sanitisedQueryString = request.queryString?.let {
      "?$it"
    } ?: ""

    logger.info(
      """
        
Request To: ${request.method} ${request.requestURI}$sanitisedQueryString
Request Body: $requestBody
Response Code: ${response.status} 
Response Body: $responseBody

""",
    )

    writeResponseToCaller(wrappedRequest, wrappedResponse)
  }

  private fun writeResponseToCaller(
    request: ContentCachingRequestWrapper,
    response: ContentCachingResponseWrapper,
  ) {
    if (request.isAsyncStarted) {
      request.asyncContext.addListener(
        object : DefaultAsyncListener {
          override fun onComplete(p0: AsyncEvent?) {
            response.copyBodyToResponse()
          }
        },
      )
    } else {
      response.copyBodyToResponse()
    }
  }

  private fun ContentCachingRequestWrapper.requestBodyString() = String(contentAsByteArray)

  private fun ContentCachingResponseWrapper.responseBodyString() = if (contentType == "application/json") {
    String(contentAsByteArray)
  } else {
    "Response Body not logged as content type is $contentType"
  }

  private interface DefaultAsyncListener : AsyncListener {

    override fun onTimeout(p0: AsyncEvent?) {
      // deliberately empty
    }

    override fun onError(p0: AsyncEvent?) {
      // deliberately empty
    }

    override fun onStartAsync(p0: AsyncEvent?) {
      // deliberately empty
    }
  }
}

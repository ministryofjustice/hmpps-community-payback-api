package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.util.ContentCachingRequestWrapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.LockService
import java.io.ByteArrayInputStream
import java.time.Duration

/**
 * Ensures that multiple requests for the given key with the same request
 * body will be handled sequentially, with the result from the first successful
 * execution being returned for all subsequent requests.
 */
@Component
class IdempotencyFilter(
  val lockService: LockService,
) : Filter {

  companion object {
    const val KEY_NAME = "Idempotency-key"

    fun calculateRequestHash(idempotencyKey: String, requestBody: ByteArray) = DigestUtils.md5DigestAsHex(idempotencyKey.toByteArray() + requestBody)
  }

  override fun doFilter(
    request: ServletRequest,
    response: ServletResponse,
    chain: FilterChain,
  ) {
    if (request !is HttpServletRequest || response !is HttpServletResponse || request.getHeader(KEY_NAME) == null) {
      chain.doFilter(request, response)
      return
    }

    val idempotencyKey = request.getHeader(KEY_NAME)

    val wrappedRequest = ContentCachingRequestWrapper(request, 0)
    val body = wrappedRequest.cacheRequest()

    val hash = calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = body,
    )

    val status = lockService.singleFlightForIntResult(
      lockKey = "lock:$hash",
      resultKey = "result:$hash",
      lockWaitTime = Duration.ofSeconds(2),
      lockLeaseTime = Duration.ofSeconds(2),
    ) {
      val readableRequest = RequestBodySupportContentWrapper(wrappedRequest)
      readableRequest.prepareInputStream()

      chain.doFilter(readableRequest, response)

      if (response.status in 200..<300) {
        LockService.SingleFlightResult.RetainValue(response.status)
      } else {
        LockService.SingleFlightResult.DontRetainValue(response.status)
      }
    }

    response.status = status
    response.setContentLength(0)
  }
}

/**
 * We must pre-read the input stream if we want to access the request before it
 * is ready by the target consumer
 */
private fun ContentCachingRequestWrapper.cacheRequest() = inputStream.readAllBytes()

/**
 * [ContentCachingRequestWrapper] does not implement the read() function required by Spring
 * MVC to support unmarshalling @RequestBody values. For that reason we have to wrap it
 * in an additional wrapper to provide the support required for @RequestBody
 *
 * This is taken from https://stackoverflow.com/a/77517457
 */
class RequestBodySupportContentWrapper(private val wrapped: ContentCachingRequestWrapper) : ContentCachingRequestWrapper(wrapped, 0) {
  private val requestBodySupportInputStreamWrapper: RequestBodySupportInputStreamWrapper by lazy {
    RequestBodySupportInputStreamWrapper(super.getInputStream())
  }

  override fun getInputStream(): ServletInputStream = requestBodySupportInputStreamWrapper

  // use the method supported by ContentCachingRequestWrapper to read the bytes
  fun prepareInputStream() = requestBodySupportInputStreamWrapper.resetReading(wrapped.contentAsByteArray)
}

class RequestBodySupportInputStreamWrapper(private val wrapped: ServletInputStream) : ServletInputStream() {

  private lateinit var bais: ByteArrayInputStream

  // convert the bytes read from the cache to InputStream
  // which later can be used in the read()
  fun resetReading(bytes: ByteArray) {
    this.bais = ByteArrayInputStream(bytes)
  }

  override fun read() = bais.read()

  override fun isFinished() = wrapped.isFinished

  override fun isReady(): Boolean = wrapped.isReady

  override fun setReadListener(readListener: ReadListener?) = wrapped.setReadListener(readListener)
}

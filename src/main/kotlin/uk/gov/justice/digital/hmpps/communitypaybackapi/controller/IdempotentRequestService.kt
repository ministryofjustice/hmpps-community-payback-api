package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.LockService
import java.time.Duration

@Component
class IdempotentRequestService(
  private val lockService: LockService,
) {
  /**
   * Ensures that multiple requests for the given key will be handled
   * sequentially, with the result from the first successful execution being
   * returned for all subsequent requests.
   *
   * Note that this doesn't fully solve idempotency issues as it only guarantees
   * that a single thread executes for a given key
   *
   * For a complete solution we need to persist the result and return that for
   * subsequent requests (outside of this locking solution). We also need to
   * ensure the executing thread reverts any side effects on failure (e.g. entities
   * created in NDelius)
   *
   * Furthermore, use of this function assumes that the requests using the same
   * idempotency key are identical. We could store a request fingerprint in redis
   * to detect differences in requests if required (and presumably error)
   */
  fun handleIdempotentRequest(
    idempotencyKey: String?,
    maxProcessingTime: Duration,
    exec: () -> Int,
  ): ResponseEntity<Unit> {
    val statusCode = if (idempotencyKey != null) {
      lockService.singleFlightForIntResult(
        lockKey = "lock:$idempotencyKey",
        resultKey = "result:$idempotencyKey",
        lockWaitTime = maxProcessingTime,
        lockLeaseTime = maxProcessingTime,
        exec = exec,
      )
    } else {
      exec.invoke()
    }

    return ResponseEntity.status(statusCode).build()
  }
}

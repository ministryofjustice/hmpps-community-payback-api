package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.IdempotencyFilter
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.LockService
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping(
  "/it/idempotency",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class IdempotencyTestEndpoints {

  @PostMapping(path = ["/success"])
  fun success() = ResponseEntity.noContent().build<Unit>()

  @PostMapping(path = ["/client-error"])
  fun clientError() = ResponseEntity.badRequest().build<Unit>()

  @PostMapping(path = ["/use-body-as-status-code"])
  fun echo(@RequestBody value: Int) = ResponseEntity.status(value).build<Unit>()
}

class IdempotencyKeyIT : IntegrationTestBase() {

  @Autowired
  lateinit var redissonClient: RedissonClient

  @Autowired
  lateinit var lockService: LockService

  @Test
  fun `if result for idempotency key already exists it should be returned without any changes made`() {
    val bodyValue = 204
    val idempotencyKey = "test:${UUID.randomUUID()}"
    val requestHash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = bodyValue.toString().toByteArray(),
    )

    redissonClient.getBucket<Int>("result:$requestHash")!!.set(205)

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(bodyValue)
      .exchange()
      .expectStatus().isEqualTo(205)
  }

  @Test
  fun `if there's an existing request in process wait on completion and return the result`() {
    val bodyValue = 204
    val idempotencyKey = "test:${UUID.randomUUID()}"
    val requestHash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = bodyValue.toString().toByteArray(),
    )

    lockService.withDistributedLock(
      key = "lock:$requestHash",
      waitTime = Duration.ofSeconds(5),
      leaseTime = Duration.ofSeconds(5),
    ) {
      runBlocking {
        delay(1000)
        redissonClient.getBucket<Int>("result:$requestHash")!!.set(206)
      }
    }

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(bodyValue)
      .exchange()
      .expectStatus().isEqualTo(206)
  }

  @Test
  fun `result is recorded when set by API call`() {
    val bodyValue = 204
    val idempotencyKey = "test:${UUID.randomUUID()}"
    val requestHash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = bodyValue.toString().toByteArray(),
    )

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(bodyValue)
      .exchange()
      .expectStatus().isEqualTo(204)

    assertThat(redissonClient.getBucket<Int>("result:$requestHash")?.get()).isEqualTo(204)
  }

  @Test
  fun `don't use prior response for same idempotency key if request body changes`() {
    val idempotencyKey = "test:${UUID.randomUUID()}"

    val request1BodyValue = 204
    val request1Hash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = request1BodyValue.toString().toByteArray(),
    )

    val request2BodyValue = 205
    val request2Hash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = request2BodyValue.toString().toByteArray(),
    )

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(request1BodyValue)
      .exchange()
      .expectStatus().isEqualTo(204)

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(request2BodyValue)
      .exchange()
      .expectStatus().isEqualTo(205)

    assertThat(redissonClient.getBucket<Int>("result:$request1Hash")?.get()).isEqualTo(204)
    assertThat(redissonClient.getBucket<Int>("result:$request2Hash")?.get()).isEqualTo(205)
  }

  @Test
  fun `result is not recorded for 4xx`() {
    val bodyValue = 400
    val idempotencyKey = "test:${UUID.randomUUID()}"
    val requestHash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = bodyValue.toString().toByteArray(),
    )

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(400)
      .exchange()
      .expectStatus().isEqualTo(400)

    assertThat(redissonClient.getBucket<Int>("result:$requestHash").get() as Any?).isNull()
  }

  @Test
  fun `result is not recorded for 5xx`() {
    val bodyValue = 500
    val idempotencyKey = "test:${UUID.randomUUID()}"
    val requestHash = IdempotencyFilter.calculateRequestHash(
      idempotencyKey = idempotencyKey,
      requestBody = bodyValue.toString().toByteArray(),
    )

    webTestClient.post()
      .uri("/it/idempotency/use-body-as-status-code")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", idempotencyKey)
      .bodyValue(bodyValue)
      .exchange()
      .expectStatus()
      .isEqualTo(500)

    assertThat(redissonClient.getBucket<Int>("result:$requestHash").get() as Any?).isNull()
  }
}

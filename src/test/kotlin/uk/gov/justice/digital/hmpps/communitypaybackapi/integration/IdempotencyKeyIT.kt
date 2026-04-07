package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.LockService
import java.time.Duration
import java.util.UUID

/**
 * These tests use the POST adjustments endpoints to test various scenarios related to
 * the idempotency key
 */
class IdempotencyKeyIT : IntegrationTestBase() {

  @Autowired
  lateinit var redissonClient: RedissonClient

  @Autowired
  lateinit var lockService: LockService

  companion object {
    const val CRN = "X123456"
    const val DELIUS_EVENT_NUMBER = 92
  }

  @Test
  fun `if result for idempotency key already exists it should be returned without any changes made`() {
    val taskId = UUID.randomUUID()

    redissonClient.getBucket<Int>("result:post-adjustment:$taskId")!!.set(205)

    webTestClient.post()
      .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", "post-adjustment:$taskId")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAdjustmentDto.valid(ctx).copy(
          taskId = taskId,
        ),
      )
      .exchange()
      .expectStatus()
      .isEqualTo(205)
  }

  @Test
  fun `if there's an existing request in process wait on completion and return the result`() {
    val taskId = UUID.randomUUID()

    lockService.withDistributedLock(
      key = "lock:post-adjustment:$taskId",
      waitTime = Duration.ofSeconds(5),
      leaseTime = Duration.ofSeconds(5),
    ) {
      runBlocking {
        delay(1000)
        redissonClient.getBucket<Int>("result:post-adjustment:$taskId")!!.set(206)
      }
    }

    webTestClient.post()
      .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", "post-adjustment:$taskId")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAdjustmentDto.valid(ctx).copy(
          taskId = taskId,
        ),
      )
      .exchange()
      .expectStatus()
      .isEqualTo(206)
  }

  @Test
  fun `result is recorded when set by API call`() {
    val appointment = AppointmentEntity.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER).persist(ctx)
    val task = AppointmentTaskEntity.valid().copy(appointment = appointment).persist(ctx)

    CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
      crn = CRN,
      case = NDCaseSummary.valid(),
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(eventNumber = DELIUS_EVENT_NUMBER),
      ),
      username = "theusername",
    )

    CommunityPaybackAndDeliusMockServer.setupPostAdjustmentResponse(username = "theusername")

    webTestClient.post()
      .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
      .addAdminUiAuthHeader("theusername")
      .header("Idempotency-key", "post-adjustment:${task.id}")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateAdjustmentDto.valid(ctx).copy(
          taskId = task.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    assertThat(redissonClient.getBucket<Int>("result:post-adjustment:${task.id}")!!.get()).isEqualTo(200)
  }
}

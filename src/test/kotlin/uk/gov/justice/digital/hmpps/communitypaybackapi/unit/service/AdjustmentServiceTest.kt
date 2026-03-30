package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import com.google.common.base.Verify.verify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentPostResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory.CreateAdjustmentEventDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentServiceTest {

  @RelaxedMockK
  private lateinit var adjustmentEventService: AdjustmentEventService

  @RelaxedMockK
  private lateinit var adjustmentValidationService: AdjustmentValidationService

  @RelaxedMockK
  private lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  val clock: Clock = ClockConfiguration.MutableClock(Instant.now())

  @InjectMockKs
  private lateinit var service: AdjustmentService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    val UNPAID_WORK_DETAILS: UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
    const val USERNAME = "username"
    val REASON_ID: UUID = UUID.fromString("74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
  }

  @Nested
  inner class CreateAdjustment {

    @Test
    fun success() {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)
      val appointmentTask = AppointmentTaskEntity.valid()

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = REASON_ID,
        minutes = 50,
      )

      val validatedAdjustment = AdjustmentValidationService.ValidatedCreateAdjustment(request, reason, appointmentTask)
      every {
        adjustmentValidationService.validateCreate(request, UNPAID_WORK_DETAILS, USERNAME)
      } returns validatedAdjustment

      every {
        communityPaybackAndDeliusClient.postAdjustments(
          username = "username",
          adjustmentRequests = listOf(
            request.toNDAdjustmentRequest(
              crn = CRN,
              deliusEventNumber = EVENT_NUMBER,
              reason = reason,
            ),
          ),
        )
      } returns listOf(NDAdjustmentPostResponse(5L))

      service.createAdjustment(
        upwDetailsId = UNPAID_WORK_DETAILS,
        createAdjustment = request,
        username = USERNAME,
      )

      verify {
        adjustmentEventService.publishCreateEvent(
          CreateAdjustmentEventDetails(
            createAdjustmentDto = request,
            appointment = appointmentTask.appointment,
            reason = validatedAdjustment.reason,
            deliusAdjustmentId = 5L,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = appointmentTask.id.toString(),
            ),
          ),
        )
      }
    }
  }
}

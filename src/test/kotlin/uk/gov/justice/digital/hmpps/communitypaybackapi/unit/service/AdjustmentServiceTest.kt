package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentPostResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator.DeleteAdjustmentProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DeleteAdjustmentResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentServiceTest {

  @RelaxedMockK
  private lateinit var adjustmentValidationService: AdjustmentValidationService

  @RelaxedMockK
  private lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  private lateinit var springEventPublisher: SpringEventPublisher

  @RelaxedMockK
  lateinit var adjustmentIdGenerator: AdjustmentIdGenerator

  @RelaxedMockK
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  val clock: Clock = ClockConfiguration.MutableClock(Instant.now())

  @InjectMockKs
  private lateinit var service: AdjustmentService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    val UNPAID_WORK_DETAILS: UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
    const val USERNAME = "username"
  }

  @Nested
  inner class GetAdjustments {
    @Test
    fun success() {
      val adjustments = listOf(
        NDAdjustment.valid(),
        NDAdjustment.valid(),
      )

      every { communityPaybackAndDeliusClient.getAdjustments(any(), any()) } returns NDAdjustmentResponse(
        adjustments = adjustments,
      )

      val results = service.getAdjustments("X123456", 1)

      assertThat(results).hasSameElementsAs(adjustments.map { it.toDto() })
    }
  }

  @Nested
  inner class CreateAdjustment {

    @Test
    fun success() {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)
      val appointment = AppointmentEntity.valid()
      val id = UUID.randomUUID()
      val dateOfAdjustment = LocalDate.now().minusDays(3)

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = reason.id,
        minutes = 50,
        adjustmentDate = dateOfAdjustment,
      )

      val validatedAdjustment = AdjustmentValidationService.ValidatedCreateAdjustment(request, reason, appointment)
      every { adjustmentIdGenerator.generateId(request) } returns id

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
              reference = id,
              dateOfAdjustment = dateOfAdjustment,
            ),
          ),
        )
      } returns listOf(NDAdjustmentPostResponse(5L))

      service.createAdjustment(
        upwDetailsId = UNPAID_WORK_DETAILS,
        createAdjustment = request,
        username = USERNAME,
      )

      verifyOrder {
        communityPaybackAndDeliusClient.deleteAdjustment(id)

        communityPaybackAndDeliusClient.postAdjustments(
          username = any(),
          adjustmentRequests = match { it.size == 1 && it.first().reference == id },
        )
      }

      verify {
        springEventPublisher.publishEvent(
          AdjustmentCreatedEvent(
            id = id,
            createDto = request,
            appointmentEntity = appointment,
            reason = validatedAdjustment.reason,
            deliusAdjustmentId = 5L,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = appointment.id.toString(),
            ),
            adjustmentDate = dateOfAdjustment,
          ),
        )
      }
    }
  }

  @Nested
  inner class DeleteAdjustment {

    @Test
    fun success() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.Success)

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify {
        springEventPublisher.publishEvent(
          AdjustmentDeletedEvent(
            id = expectedId,
            eventToDelete = expectedEvent,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = USERNAME,
            ),
          ),
        )
      }
    }

    @Test
    fun `returns not found if no entity with correct ID is in repository`() {
      val adjustmentId = UUID.randomUUID()
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns null

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.NotFound)

      verify(exactly = 0) {
        communityPaybackAndDeliusClient.deleteAdjustment(any())

        springEventPublisher.publishEvent(any())
      }
    }

    @Test
    fun `returns not found if upstream client returns 404`() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      every { communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId) } throws WebClientResponseExceptionFactory.notFound()

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.NotFound)

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify(exactly = 0) {
        springEventPublisher.publishEvent(any())
      }
    }

    @Test
    fun `returns failure if upstream client returns error response`() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      every { communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId) } throws WebClientResponseExceptionFactory.badRequest("Some error")

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isInstanceOf(DeleteAdjustmentResult.Failed::class.java)
      assertThat((result as DeleteAdjustmentResult.Failed).exception.statusCode.value()).isEqualTo(400)
      assertThat(result.exception.responseBodyAsString).isEqualTo("Some error")

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify(exactly = 0) {
        springEventPublisher.publishEvent(any())
      }
    }
  }
}

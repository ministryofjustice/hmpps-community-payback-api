package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AutomatedTravelTimeService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.CreateSystemAdjustmentResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DeleteAdjustmentResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AutomatedTravelTimeServiceTest {

  @RelaxedMockK
  private lateinit var adjustmentService: AdjustmentService

  @RelaxedMockK
  private lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @RelaxedMockK
  private lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @RelaxedMockK
  private lateinit var appointmentEntityRepository: AppointmentEntityRepository

  private val clock: Clock = ClockConfiguration.MutableClock(Instant.now())

  private lateinit var service: AutomatedTravelTimeService

  private val travelTimeReason = AdjustmentReasonEntity.valid().copy(deliusCode = "TTX", needsLinkToAppointment = true)
  private val appointment = AppointmentEntity.valid()

  private companion object {
    const val USERNAME = "the-user"
    const val PROVIDER_CODE = "N07"
    val ATTENDED: ContactOutcomeEntity = ContactOutcomeEntity.valid().copy(attended = true)
    val NOT_ATTENDED: ContactOutcomeEntity = ContactOutcomeEntity.valid().copy(attended = false)
  }

  @BeforeEach
  fun setup() {
    service = buildService(providerCodes = listOf(PROVIDER_CODE))
    every { adjustmentReasonEntityRepository.findByDeliusCode("TTX") } returns travelTimeReason
    every {
      adjustmentService.createSystemAdjustment(any(), any(), any(), any())
    } answers { CreateSystemAdjustmentResult.Success(createdAdjustment()) }
    every { adjustmentService.deleteAdjustment(any(), any(), any()) } returns DeleteAdjustmentResult.Success
  }

  private fun buildService(providerCodes: List<String>, enabled: Boolean = true) = AutomatedTravelTimeService(
    adjustmentService,
    adjustmentEventEntityRepository,
    adjustmentReasonEntityRepository,
    clock,
    appointmentEntityRepository,
    providerCodes,
    enabled,
  )

  private fun eligibleProject(group: ProjectTypeGroupDto? = ProjectTypeGroupDto.GROUP) = ProjectDto.valid().copy(
    providerCode = PROVIDER_CODE,
    projectType = ProjectTypeDto.valid().copy(group = group),
  )

  private fun createdAdjustment() = AdjustmentEntity(
    id = UUID.randomUUID(),
    deliusId = 1L,
    crn = appointment.crn,
    deliusEventNumber = appointment.deliusEventNumber,
    type = AdjustmentType.POSITIVE,
    minutes = 60,
    date = appointment.date,
    reason = travelTimeReason,
    appointment = appointment,
  )

  private fun existingCreateEvent() = AdjustmentEventEntity.valid().copy(
    eventType = AdjustmentEventType.CREATE,
    triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
    appointment = appointment,
    adjustmentReason = travelTimeReason,
  )

  private fun givenExistingAdjustment(createEvent: AdjustmentEventEntity?) {
    every {
      adjustmentEventEntityRepository.findNondeletedCreateEvents(
        appointment,
        AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
      )
    } returns listOfNotNull(createEvent)
  }

  private fun updatedEvent(
    outcome: ContactOutcomeEntity?,
    project: ProjectDto = eligibleProject(),
  ) = AppointmentUpdatedEvent.valid().copy(
    appointmentEntity = appointment,
    updateDto = ValidatedAppointment.validUpdateAppointment().copy(contactOutcome = outcome, project = project),
    trigger = AppointmentEventTrigger.valid().copy(triggeredBy = USERNAME),
  )

  private fun createdEvent(
    outcome: ContactOutcomeEntity?,
    project: ProjectDto = eligibleProject(),
  ) = AppointmentCreatedEvent.valid().copy(
    appointmentEntity = appointment,
    createDto = ValidatedAppointment.validCreateAppointment().copy(contactOutcome = outcome, project = project),
    trigger = AppointmentEventTrigger.valid().copy(triggeredBy = USERNAME),
  )

  private fun verifyNothingCreatedOrDeleted() {
    verify(exactly = 0) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
    verify(exactly = 0) { adjustmentService.deleteAdjustment(any(), any(), any()) }
  }

  @Nested
  inner class Eligibility {

    @Test
    fun `disabled feature neither creates nor deletes adjustments for configured providers`() {
      service = buildService(providerCodes = listOf(PROVIDER_CODE), enabled = false)
      givenExistingAdjustment(existingCreateEvent())

      service.onAppointmentCreated(createdEvent(ATTENDED))
      service.onAppointmentUpdated(updatedEvent(ATTENDED))
      service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED))
      service.onAppointmentUpdated(updatedEvent(null))

      verifyNothingCreatedOrDeleted()
      verify(exactly = 0) { adjustmentEventEntityRepository.findNondeletedCreateEvents(any(), any()) }
    }

    @Test
    fun `does nothing when no provider codes are configured`() {
      service = buildService(providerCodes = emptyList())
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(ATTENDED))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `applies to any configured provider`() {
      service = buildService(providerCodes = listOf("N56", PROVIDER_CODE))
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(ATTENDED, project = eligibleProject().copy(providerCode = "N56")))

      verify(exactly = 1) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
    }

    @Test
    fun `does nothing for providers that are not configured`() {
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(ATTENDED, project = eligibleProject().copy(providerCode = "N56")))

      verifyNothingCreatedOrDeleted()
    }

    @ParameterizedTest
    @CsvSource("ETE", "OTHER_ETE", "INDUCTION")
    fun `does nothing for ineligible project types`(group: ProjectTypeGroupDto) {
      givenExistingAdjustment(existingCreateEvent())

      service.onAppointmentUpdated(updatedEvent(ATTENDED, project = eligibleProject(group)))
      service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED, project = eligibleProject(group)))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `does nothing when project type has no group`() {
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(ATTENDED, project = eligibleProject(group = null)))

      verifyNothingCreatedOrDeleted()
    }
  }

  @Nested
  inner class AppointmentCreation {

    @ParameterizedTest
    @CsvSource("GROUP", "INDIVIDUAL")
    fun `eligible attended appointment creates a 60 minute travel time adjustment`(group: ProjectTypeGroupDto) {
      givenExistingAdjustment(null)

      val dto = slot<CreateAdjustmentDto>()
      val trigger = slot<AdjustmentEventTrigger>()
      every {
        adjustmentService.createSystemAdjustment(any(), capture(dto), any(), capture(trigger))
      } returns CreateSystemAdjustmentResult.Success(createdAdjustment())

      service.onAppointmentCreated(createdEvent(ATTENDED, project = eligibleProject(group)))

      verify(exactly = 1) {
        adjustmentService.createSystemAdjustment(
          UnpaidWorkDetailsIdDto(appointment.crn, appointment.deliusEventNumber),
          any(),
          USERNAME,
          any(),
        )
      }
      assertThat(dto.captured).isEqualTo(
        CreateAdjustmentDto(
          taskId = null,
          type = CreateAdjustmentTypeDto.Positive,
          minutes = 60,
          adjustmentReasonId = travelTimeReason.id,
          adjustmentDate = appointment.date,
          appointmentId = appointment.id,
        ),
      )
      assertThat(trigger.captured).isEqualTo(
        AdjustmentEventTrigger(
          triggeredAt = OffsetDateTime.now(clock),
          triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
          triggeredBy = USERNAME,
        ),
      )
    }

    @Test
    fun `non-attendance does not create an adjustment`() {
      givenExistingAdjustment(null)

      service.onAppointmentCreated(createdEvent(NOT_ATTENDED))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `no outcome does not create an adjustment`() {
      givenExistingAdjustment(null)

      service.onAppointmentCreated(createdEvent(null))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `missing travel time reason fails before creating an adjustment`() {
      givenExistingAdjustment(null)
      every { adjustmentReasonEntityRepository.findByDeliusCode("TTX") } returns null

      assertThatThrownBy { service.onAppointmentCreated(createdEvent(ATTENDED)) }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Could not find adjustment reason with code 'TTX'")

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `upstream creation failure is propagated`() {
      givenExistingAdjustment(null)
      val failure = WebClientResponseExceptionFactory.badRequest("boom")
      every { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) } throws failure

      assertThatThrownBy { service.onAppointmentCreated(createdEvent(ATTENDED)) }
        .isSameAs(failure)
    }

    @Test
    fun `replayed creation event does not duplicate an existing adjustment`() {
      givenExistingAdjustment(existingCreateEvent())

      service.onAppointmentCreated(createdEvent(ATTENDED))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `validation failure does not throw`() {
      givenExistingAdjustment(null)
      every {
        adjustmentService.createSystemAdjustment(any(), any(), any(), any())
      } returns CreateSystemAdjustmentResult.ValidationFailed(
        listOf(ValidationResultItem("$.minutes", "EXCEEDS_REMAINING_REQUIREMENT_TIME", emptyMap())),
      )

      service.onAppointmentCreated(createdEvent(ATTENDED))

      verify(exactly = 1) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
    }
  }

  @Nested
  inner class AppointmentUpdates {

    @Test
    fun `uses updated date when recording attendance and locks before checking existing events`() {
      givenExistingAdjustment(null)
      val event = updatedEvent(ATTENDED)
      appointment.date = event.updateDto.dto.date.plusDays(10)

      service.onAppointmentUpdated(event)

      verifyOrder {
        appointmentEntityRepository.lockById(appointment.id)
        adjustmentEventEntityRepository.findNondeletedCreateEvents(appointment, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
        adjustmentService.createSystemAdjustment(any(), match { it.adjustmentDate == event.updateDto.dto.date }, any(), any())
      }
      assertThat(appointment.date).isEqualTo(event.updateDto.dto.date)
    }

    @Test
    fun `attended to attended leaves existing adjustment unchanged`() {
      givenExistingAdjustment(existingCreateEvent())

      service.onAppointmentUpdated(updatedEvent(ATTENDED))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `attended to non-attendance deletes linked adjustment`() {
      val createEvent = existingCreateEvent()
      givenExistingAdjustment(createEvent)

      service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED))

      verify(exactly = 1) { adjustmentService.deleteAdjustment(createEvent.id, USERNAME, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME) }
      verify(exactly = 0) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
    }

    @Test
    fun `attended to no outcome deletes linked adjustment`() {
      val createEvent = existingCreateEvent()
      givenExistingAdjustment(createEvent)

      service.onAppointmentUpdated(updatedEvent(null))

      verify(exactly = 1) { adjustmentService.deleteAdjustment(createEvent.id, USERNAME, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME) }
    }

    @Test
    fun `non-attendance to attended creates adjustment`() {
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(ATTENDED))

      verify(exactly = 1) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
      verify(exactly = 0) { adjustmentService.deleteAdjustment(any(), any(), any()) }
    }

    @Test
    fun `non-attendance to non-attendance takes no action`() {
      givenExistingAdjustment(null)

      service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED))

      verifyNothingCreatedOrDeleted()
    }

    @Test
    fun `adjustment not found when deleting does not throw`() {
      givenExistingAdjustment(existingCreateEvent())
      every { adjustmentService.deleteAdjustment(any(), any(), any()) } returns DeleteAdjustmentResult.NotFound

      service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED))

      verify(exactly = 1) { adjustmentService.deleteAdjustment(any(), any(), any()) }
    }

    @Test
    fun `upstream failure deleting adjustment is thrown`() {
      givenExistingAdjustment(existingCreateEvent())
      every {
        adjustmentService.deleteAdjustment(any(), any(), any())
      } returns DeleteAdjustmentResult.Failed(WebClientResponseExceptionFactory.badRequest("boom"))

      assertThatThrownBy { service.onAppointmentUpdated(updatedEvent(NOT_ATTENDED)) }
        .isInstanceOf(InternalServerErrorException::class.java)
    }
  }

  @Nested
  inner class Idempotency {

    @Test
    fun `processing the same attended event twice creates only one adjustment`() {
      val event = updatedEvent(ATTENDED)

      givenExistingAdjustment(null)
      service.onAppointmentUpdated(event)

      givenExistingAdjustment(existingCreateEvent())
      service.onAppointmentUpdated(event)

      verify(exactly = 1) { adjustmentService.createSystemAdjustment(any(), any(), any(), any()) }
    }
  }
}

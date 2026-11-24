package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AutomatedTravelTimeService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DeleteAdjustmentResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AutomatedTravelTimeServiceTest {

  @RelaxedMockK
  private lateinit var adjustmentService: AdjustmentService

  @RelaxedMockK
  private lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @RelaxedMockK
  private lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @RelaxedMockK
  private lateinit var sentryService: SentryService

  private lateinit var service: AutomatedTravelTimeService

  private companion object {
    const val LONDON = "N07"
    const val USERNAME = "the.user"
  }

  private val travelTimeReason = AdjustmentReasonEntity.valid().copy(deliusCode = "TTX")
  private val appointmentDate = LocalDate.now().minusDays(2)
  private val appointment = AppointmentEntity.valid().copy(date = appointmentDate)

  @BeforeEach
  fun setup() {
    setupService(providerCodes = listOf(LONDON))

    every { adjustmentReasonEntityRepository.findByDeliusCode("TTX") } returns travelTimeReason
    every { adjustmentEventEntityRepository.findNondeletedCreateEvents(any(), any()) } returns emptyList()
    every { adjustmentService.deleteAdjustment(any(), any(), any()) } returns DeleteAdjustmentResult.Success
  }

  private fun setupService(providerCodes: List<String>) {
    service = AutomatedTravelTimeService(
      adjustmentService = adjustmentService,
      providerCodes = providerCodes,
      clock = ClockConfiguration.MutableClock(Instant.now()),
      adjustmentEventEntityRepository = adjustmentEventEntityRepository,
      adjustmentReasonEntityRepository = adjustmentReasonEntityRepository,
      sentryService = sentryService,
    )
  }

  private fun project(
    providerCode: String = LONDON,
    group: ProjectTypeGroupDto? = ProjectTypeGroupDto.GROUP,
  ) = ProjectDto.valid().copy(
    providerCode = providerCode,
    projectType = ProjectTypeDto.valid().copy(group = group),
  )

  private fun outcome(attended: Boolean) = ContactOutcomeEntity.valid().copy(attended = attended)

  private fun createdEvent(
    outcome: ContactOutcomeEntity? = outcome(attended = true),
    project: ProjectDto = project(),
  ) = AppointmentCreatedEvent.valid().copy(
    appointmentEntity = appointment,
    createDto = ValidatedAppointment.validCreateAppointment().copy(contactOutcome = outcome, project = project),
    trigger = AppointmentEventTrigger.valid().copy(triggeredBy = USERNAME),
  )

  private fun updatedEvent(
    outcome: ContactOutcomeEntity? = outcome(attended = true),
    project: ProjectDto = project(),
    date: LocalDate = appointmentDate,
  ) = AppointmentUpdatedEvent.valid().copy(
    appointmentEntity = appointment,
    updateDto = ValidatedAppointment.validUpdateAppointment().let {
      it.copy(dto = it.dto.copy(date = date), contactOutcome = outcome, project = project)
    },
    trigger = AppointmentEventTrigger.valid().copy(triggeredBy = USERNAME),
  )

  private fun existingAutomatedAdjustment(): AdjustmentEventEntity {
    val createEvent = AdjustmentEventEntity.valid().copy(
      eventType = AdjustmentEventType.CREATE,
      triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
      appointment = appointment,
    )
    every {
      adjustmentEventEntityRepository.findNondeletedCreateEvents(appointment, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
    } returns listOf(createEvent)
    return createEvent
  }

  private fun verifyTravelTimeCreated(adjustmentDate: LocalDate = appointmentDate) {
    verify(exactly = 0) { sentryService.captureException(any()) }
    verify(exactly = 1) {
      adjustmentService.createAdjustment(
        upwDetailsId = UnpaidWorkDetailsIdDto(appointment.crn, appointment.deliusEventNumber),
        createAdjustment = CreateAdjustmentDto(
          taskId = null,
          type = CreateAdjustmentTypeDto.Negative,
          minutes = 60,
          adjustmentReasonId = travelTimeReason.id,
          adjustmentDate = adjustmentDate,
          appointmentId = appointment.id,
        ),
        username = USERNAME,
        adjustmentEventTriggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
      )
    }
  }

  private fun verifyCreationFailureReported(exception: Exception) {
    verify(exactly = 1) {
      sentryService.captureException(
        match {
          it.message == "Failed to create automated travel time adjustment for Delius appointment ${appointment.deliusId}" &&
            it.cause === exception
        },
      )
    }
  }

  private fun verifyNoAdjustmentChanges() {
    verify(exactly = 0) { sentryService.captureException(any()) }
    verify(exactly = 0) { adjustmentService.createAdjustment(any(), any(), any(), any()) }
    verify(exactly = 0) { adjustmentService.deleteAdjustment(any(), any(), any()) }
  }

  @Nested
  inner class Eligibility {

    @Test
    fun `no provider codes configured, do nothing`() {
      setupService(providerCodes = emptyList())

      service.onAppointmentCreated(createdEvent())

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `provider not configured, do nothing`() {
      existingAutomatedAdjustment()

      service.onAppointmentCreated(createdEvent(project = project(providerCode = "N03")))
      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false), project = project(providerCode = "N03")))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `any configured provider is eligible`() {
      setupService(providerCodes = listOf("N03", LONDON))

      service.onAppointmentCreated(createdEvent(project = project(providerCode = "N03")))

      verifyTravelTimeCreated()
    }

    @ParameterizedTest
    @EnumSource(value = ProjectTypeGroupDto::class, names = ["ETE", "OTHER_ETE", "INDUCTION"])
    fun `project type group doesn't support travel time, do nothing`(group: ProjectTypeGroupDto) {
      existingAutomatedAdjustment()

      service.onAppointmentCreated(createdEvent(project = project(group = group)))
      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false), project = project(group = group)))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `project type has no group, do nothing`() {
      service.onAppointmentCreated(createdEvent(project = project(group = null)))

      verifyNoAdjustmentChanges()
    }
  }

  @Nested
  inner class OnAppointmentCreated {

    @ParameterizedTest
    @EnumSource(value = ProjectTypeGroupDto::class, names = ["GROUP", "INDIVIDUAL"])
    fun `attended outcome creates a 60 minute travel time adjustment`(group: ProjectTypeGroupDto) {
      service.onAppointmentCreated(createdEvent(project = project(group = group)))

      verifyTravelTimeCreated()
    }

    @Test
    fun `non-attended outcome, do nothing`() {
      service.onAppointmentCreated(createdEvent(outcome = outcome(attended = false)))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `no outcome, do nothing`() {
      service.onAppointmentCreated(createdEvent(outcome = null))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `automated adjustment already exists, do not create another`() {
      existingAutomatedAdjustment()

      service.onAppointmentCreated(createdEvent())

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `missing travel time adjustment reason throws`() {
      every { adjustmentReasonEntityRepository.findByDeliusCode("TTX") } returns null

      assertThatThrownBy { service.onAppointmentCreated(createdEvent()) }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Could not find adjustment reason with code 'TTX'")

      verify(exactly = 1) {
        sentryService.captureException(
          match { it.cause is IllegalStateException && it.cause?.message == "Could not find adjustment reason with code 'TTX'" },
        )
      }
    }

    @Test
    fun `validation failure creating the adjustment is reported to Sentry and propagated`() {
      val exception = BadRequestException("Credited minutes exceeds the remaining time")
      every { adjustmentService.createAdjustment(any(), any(), any(), any()) } throws exception

      assertThatThrownBy { service.onAppointmentCreated(createdEvent()) }.isSameAs(exception)

      verifyCreationFailureReported(exception)
    }
  }

  @Nested
  inner class OnAppointmentUpdated {

    @Test
    fun `non-attended to attended creates adjustment`() {
      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = true)))

      verifyTravelTimeCreated()
    }

    @Test
    fun `upstream failure creating the adjustment is reported to Sentry and propagated`() {
      val exception = WebClientResponseExceptionFactory.internalServerError()
      every { adjustmentService.createAdjustment(any(), any(), any(), any()) } throws exception

      assertThatThrownBy { service.onAppointmentUpdated(updatedEvent()) }.isSameAs(exception)

      verifyCreationFailureReported(exception)
    }

    @Test
    fun `adjustment is dated using the updated appointment date`() {
      val newDate = appointmentDate.minusDays(5)

      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = true), date = newDate))

      assertThat(appointment.date).isEqualTo(newDate)
      verifyTravelTimeCreated(adjustmentDate = newDate)
    }

    @Test
    fun `attended to attended leaves the existing adjustment unchanged`() {
      existingAutomatedAdjustment()

      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = true)))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `attended to non-attended deletes the automated adjustment`() {
      val createEvent = existingAutomatedAdjustment()

      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false)))

      verify(exactly = 1) {
        adjustmentService.deleteAdjustment(createEvent.id, USERNAME, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
      }
      verify(exactly = 0) { adjustmentService.createAdjustment(any(), any(), any(), any()) }
    }

    @Test
    fun `attended to no outcome deletes the automated adjustment`() {
      val createEvent = existingAutomatedAdjustment()

      service.onAppointmentUpdated(updatedEvent(outcome = null))

      verify(exactly = 1) {
        adjustmentService.deleteAdjustment(createEvent.id, USERNAME, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)
      }
    }

    @Test
    fun `non-attended to non-attended, do nothing`() {
      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false)))

      verifyNoAdjustmentChanges()
    }

    @Test
    fun `adjustment not found upstream when deleting does not throw`() {
      existingAutomatedAdjustment()
      every { adjustmentService.deleteAdjustment(any(), any(), any()) } returns DeleteAdjustmentResult.NotFound

      service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false)))

      verify(exactly = 1) { adjustmentService.deleteAdjustment(any(), any(), any()) }
    }

    @Test
    fun `upstream failure deleting adjustment throws`() {
      val createEvent = existingAutomatedAdjustment()
      every { adjustmentService.deleteAdjustment(any(), any(), any()) } returns
        DeleteAdjustmentResult.Failed(WebClientResponseExceptionFactory.internalServerError())

      assertThatThrownBy { service.onAppointmentUpdated(updatedEvent(outcome = outcome(attended = false))) }
        .isInstanceOf(InternalServerErrorException::class.java)
        .hasMessage("Failed to delete travel time adjustment '${createEvent.id}'. Upstream response is 'the body'")
    }
  }
}

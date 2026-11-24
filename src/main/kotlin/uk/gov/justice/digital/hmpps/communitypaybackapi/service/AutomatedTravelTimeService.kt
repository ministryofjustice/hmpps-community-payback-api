package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.InternalServerErrorException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import java.time.Clock

@Service
@ConditionalOnProperty(name = ["adjustments.automated-travel-time.enabled"], havingValue = "true")
class AutomatedTravelTimeService(
  private val adjustmentService: AdjustmentService,
  @Value("\${adjustments.automated-travel-time.provider-codes:}") private val providerCodes: List<String>,
  private val clock: Clock,
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
) {
  companion object {
    const val TRAVEL_TIME_REASON_CODE = "TTX"
    const val TRAVEL_TIME_MINUTES = 60

    private val log = LoggerFactory.getLogger(AutomatedTravelTimeService::class.java)
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRED)
  fun onAppointmentCreated(event: AppointmentCreatedEvent) = processAppointmentAttendance(
    appointment = event.appointmentEntity,
    project = event.createDto.project,
    outcome = event.createDto.contactOutcome,
    username = event.trigger.triggeredBy,
  )

  @EventListener
  @Transactional(Transactional.TxType.REQUIRED)
  fun onAppointmentUpdated(event: AppointmentUpdatedEvent) = processAppointmentAttendance(
    appointment = event.appointmentEntity.apply { date = event.updateDto.dto.date },
    project = event.updateDto.project,
    outcome = event.updateDto.contactOutcome,
    username = event.trigger.triggeredBy,
  )

  private fun processAppointmentAttendance(
    appointment: AppointmentEntity,
    project: ProjectDto,
    outcome: ContactOutcomeEntity?,
    username: String,
  ) {
    if (project.providerCode !in providerCodes) return
    if (!project.supportsTravelTime()) return

    val existingCreateEvent = findExistingTravelTimeCreateEvent(appointment)

    if (outcome?.attended == true) {
      if (existingCreateEvent == null) {
        createTravelTimeAdjustment(appointment, username)
      }
      return
    }

    if (existingCreateEvent != null) {
      deleteTravelTimeAdjustment(existingCreateEvent, username)
    }
  }

  private fun findExistingTravelTimeCreateEvent(appointment: AppointmentEntity) = adjustmentEventEntityRepository.findNondeletedCreateEvents(
    appointment = appointment,
    triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
  ).firstOrNull()

  private fun createTravelTimeAdjustment(
    appointment: AppointmentEntity,
    username: String,
  ) {
    val travelTimeReason = adjustmentReasonEntityRepository.findByDeliusCode(TRAVEL_TIME_REASON_CODE)
      ?: error("Could not find adjustment reason with code '$TRAVEL_TIME_REASON_CODE'")

    adjustmentService.createAdjustment(
      upwDetailsId = UnpaidWorkDetailsIdDto(appointment.crn, appointment.deliusEventNumber),
      createAdjustment = CreateAdjustmentDto(
        taskId = null,
        type = CreateAdjustmentTypeDto.Negative,
        minutes = TRAVEL_TIME_MINUTES,
        adjustmentReasonId = travelTimeReason.id,
        adjustmentDate = appointment.date,
        appointmentId = appointment.id,
      ),
      username = username,
      adjustmentEventTriggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
    )
  }

  private fun deleteTravelTimeAdjustment(
    createEvent: AdjustmentEventEntity,
    username: String,
  ) {
    val result = adjustmentService.deleteAdjustment(
      createEvent.id,
      username,
      AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
    )
    if (result is DeleteAdjustmentResult.Failed) {
      throw InternalServerErrorException(
        "Failed to delete travel time adjustment '${createEvent.id}'. Upstream response is '${result.exception.responseBodyAsString}'",
      )
    }
  }

  private fun ProjectDto.supportsTravelTime() = projectType.group
    ?.let { ProjectTypeGroup.fromDto(it).travelTimeSupported } == true
}

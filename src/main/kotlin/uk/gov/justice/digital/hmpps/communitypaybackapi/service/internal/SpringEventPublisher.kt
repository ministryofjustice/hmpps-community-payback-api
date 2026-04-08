package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService

@Service
class SpringEventPublisher(
  val applicationEventPublisher: ApplicationEventPublisher,
) {

  /**
   * publish a synchronously consumed event
   */
  fun publishEvent(event: CommunityPaybackSpringEvent) {
    applicationEventPublisher.publishEvent(event)
  }
}

sealed interface CommunityPaybackSpringEvent {

  data class AppointmentCreatedEvent(
    val createDto: AppointmentValidationService.ValidatedAppointment<CreateAppointmentDto>,
    val appointmentEntity: AppointmentEntity,
    val trigger: AppointmentEventTrigger,
  ) : CommunityPaybackSpringEvent {
    companion object
  }

  data class AdjustmentCreatedEvent(
    val createDto: CreateAdjustmentDto,
    val appointmentEntity: AppointmentEntity,
    val reason: AdjustmentReasonEntity,
    val deliusAdjustmentId: Long,
    val trigger: AdjustmentEventTrigger,
  ) : CommunityPaybackSpringEvent {
    companion object
  }

  data class AppointmentUpdatedEvent(
    val updateDto: AppointmentValidationService.ValidatedAppointment<UpdateAppointmentOutcomeDto>,
    val appointmentEntity: AppointmentEntity,
    val existingAppointment: AppointmentDto,
    val trigger: AppointmentEventTrigger,
  ) : CommunityPaybackSpringEvent {
    companion object
  }

  /**
   * Used to indicate that any NDelius entities/changes applied for a
   * prior event should be rolled back. This will typically be raised
   * when the corresponding thread of execution cannot complete
   * (e.g. database transaction fails to commit)
   *
   * The handler function should _not_ be transacted to minimise the
   * chance of failure (and this shouldn't be required anyway if
   * just dealing with NDelius)
   */
  data class NDeliusRollbackRequired(
    val event: CommunityPaybackSpringEvent,
  ) : CommunityPaybackSpringEvent
}

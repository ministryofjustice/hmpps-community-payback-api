package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent

fun AppointmentCreatedEvent.Companion.valid() = AppointmentCreatedEvent(
  createDto = ValidatedAppointment.validCreateAppointment(),
  appointmentEntity = AppointmentEntity.valid(),
  trigger = AppointmentEventTrigger.valid(),
)

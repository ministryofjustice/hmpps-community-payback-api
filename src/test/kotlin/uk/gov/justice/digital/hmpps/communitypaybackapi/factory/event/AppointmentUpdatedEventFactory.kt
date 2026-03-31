package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.UpdateAppointmentEvent

fun UpdateAppointmentEvent.Companion.valid() = UpdateAppointmentEvent(
  updateDto = AppointmentValidationService.ValidatedAppointment.validUpdateAppointment(),
  appointmentEntity = AppointmentEntity.valid(),
  existingAppointment = AppointmentDto.valid(),
  trigger = AppointmentEventTrigger.valid(),
)

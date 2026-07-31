package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment

fun ValidatedAppointment.Companion.validCreateAppointment() = ValidatedAppointment(
  dto = CreateAppointmentDto.valid(),
  minutesToCredit = randomDuration(),
  contactOutcome = ContactOutcomeEntity.valid(),
  pickUpLocation = PickUpLocationDto.valid(),
  project = ProjectDto.valid(),
)

fun ValidatedAppointment.Companion.validUpdateAppointment() = ValidatedAppointment(
  dto = UpdateAppointmentDto.valid(),
  minutesToCredit = randomDuration(),
  contactOutcome = ContactOutcomeEntity.valid(),
  pickUpLocation = PickUpLocationDto.valid(),
  project = ProjectDto.valid(),
)

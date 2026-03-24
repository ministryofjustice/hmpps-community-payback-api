package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.Validated

fun Validated.Companion.validCreateAppointment() = Validated(
  value = CreateAppointmentDto.valid(),
  minutesToCredit = randomDuration(),
  contactOutcome = ContactOutcomeEntity.valid(),
  project = ProjectDto.valid(),
)

fun Validated.Companion.validUpdateAppointment() = Validated(
  value = UpdateAppointmentOutcomeDto.valid(),
  minutesToCredit = randomDuration(),
  contactOutcome = ContactOutcomeEntity.valid(),
  project = ProjectDto.valid(),
)

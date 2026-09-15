package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import java.time.Duration

data class ValidatedAppointment<T>(
  val dto: T,
  val minutesToCredit: Duration? = null,
  val contactOutcome: ContactOutcomeEntity? = null,
  val pickUpLocation: PickUpLocationDto?,
  val project: ProjectDto,
) {
  companion object
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import kotlin.random.Random

fun CourseCompletionOutcomeDto.Companion.valid() = CourseCompletionOutcomeDto(
  crn = String.random(1).uppercase() + Random.nextInt(0, 99999),
  appointmentIdToUpdate = null,
  minutesToCredit = Long.random(0, 181),
  contactOutcome = String.random(20),
  projectCode = String.random(20),
)

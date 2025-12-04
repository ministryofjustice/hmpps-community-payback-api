package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import kotlin.Long

fun AppointmentSummaryDto.Companion.valid() = AppointmentSummaryDto(
  id = Long.random(),
  contactOutcome = ContactOutcomeDto.valid(),
  requirementMinutes = Int.random(),
  adjustmentMinutes = Int.random(),
  completedMinutes = Int.random(),
  offender = OffenderDto.OffenderLimitedDto("CRN01"),
)

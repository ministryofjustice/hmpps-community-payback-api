package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectOutcomeSummaryDto

fun NDProjectOutcomeSummary.toDto() = ProjectOutcomeSummaryDto(
  projectName = this.name,
  projectCode = this.code,
  location = this.location.toDto(),
  numberOfAppointmentsOverdue = this.overdueOutcomesCount,
  oldestOverdueAppointmentInDays = this.oldestOverdueInDays,
)

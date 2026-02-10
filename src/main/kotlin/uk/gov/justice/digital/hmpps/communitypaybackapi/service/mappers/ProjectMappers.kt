package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectSummaryDto

fun NDProject.toDto() = ProjectSummaryDto(
  projectName = this.name,
  projectCode = this.code,
  location = this.location.toDto(),
  numberOfAppointmentsOverdue = this.overdueOutcomesCount ?: 0,
  oldestOverdueAppointmentInDays = this.oldestOverdueInDays ?: 0,
)

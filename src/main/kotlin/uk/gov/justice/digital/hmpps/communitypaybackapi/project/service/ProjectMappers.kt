package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummaryDto

fun ProjectSessionSummaries.toDto() = SessionSummariesDto(this.sessions.map { it.toDto() })
fun ProjectSummary.toDto() = SessionSummaryDto(
  id = this.id,
  projectId = this.projectId,
  projectName = this.projectName,
  projectCode = this.projectCode,
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  numberOfOffendersAllocated = this.allocatedCount,
  numberOfOffendersWithOutcomes = this.compliedOutcomeCount,
  numberOfOffendersWithEA = this.enforcementActionNeededCount,
)

fun ProjectSession.toDto(offenderInfoResults: List<OffenderInfoResult>) = SessionDto(
  projectCode = this.projectCode,
  projectName = this.projectName,
  projectLocation = this.projectLocation,
  startTime = this.sessionStartTime,
  endTime = this.sessionEndTime,
  date = this.date,
  appointmentSummaries = this.appointmentSummaries.toDtos(offenderInfoResults),
)

fun List<ProjectAppointmentSummary>.toDtos(offenderInfoResults: List<OffenderInfoResult>) = this.map { it.toDto(offenderInfoResults) }

fun ProjectAppointmentSummary.toDto(
  offenderInfoResults: List<OffenderInfoResult>,
) = AppointmentSummaryDto(
  id = this.appointmentId,
  requirementMinutes = this.requirementMinutes,
  completedMinutes = this.completedMinutes,
  offender = offenderInfoResults.first { it.crn == this.crn }.toDto(),
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummaryDto

fun ProjectSessionSummaries.toDto() = SessionSummariesDto(this.sessions.map { it.toDto() })
fun ProjectSessionSummary.toDto() = SessionSummaryDto(
  id = 0,
  projectId = 0,
  projectName = this.project.name,
  projectCode = this.project.code,
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  numberOfOffendersAllocated = this.allocatedCount,
  numberOfOffendersWithOutcomes = this.compliedOutcomeCount,
  numberOfOffendersWithEA = this.enforcementActionNeededCount,
)

fun ProjectSession.toDto(offenderInfoResults: List<OffenderInfoResult>) = SessionDto(
  projectCode = this.project.code,
  projectName = this.project.name,
  projectLocation = this.project.location.toFullAddress(),
  startTime = this.startTime,
  endTime = this.endTime,
  date = this.date,
  appointmentSummaries = this.appointmentSummaries.toDtos(offenderInfoResults),
)

fun List<ProjectAppointmentSummary>.toDtos(offenderInfoResults: List<OffenderInfoResult>) = this.map { it.toDto(offenderInfoResults) }

fun ProjectAppointmentSummary.toDto(
  offenderInfoResults: List<OffenderInfoResult>,
) = AppointmentSummaryDto(
  id = this.id,
  requirementMinutes = this.requirementProgress.requirementMinutes,
  completedMinutes = this.requirementProgress.completedMinutes,
  offender = offenderInfoResults.first { it.crn == this.case.crn }.toDto(),
)

fun ProjectLocation.toFullAddress() = listOfNotNull(
  this.buildingName,
  listOfNotNull(
    this.addressNumber,
    this.streetName,
  ).joinToString(" "),
  this.townCity,
  this.county,
  this.postCode,
).joinToString(", ")

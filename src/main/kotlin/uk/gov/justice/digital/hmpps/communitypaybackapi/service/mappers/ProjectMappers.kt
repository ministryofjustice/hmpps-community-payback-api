package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult

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

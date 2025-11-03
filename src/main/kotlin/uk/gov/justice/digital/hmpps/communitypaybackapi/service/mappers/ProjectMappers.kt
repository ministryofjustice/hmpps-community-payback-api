package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult

@Service
class ProjectMappers(
  val appointmentMappers: AppointmentMappers,
) {

  fun toDto(
    session: ProjectSession,
    offenderInfoResults: List<OffenderInfoResult>,
  ) = SessionDto(
    projectCode = session.project.code,
    projectName = session.project.name,
    projectLocation = session.project.location.toFullAddress(),
    location = session.project.location.toDto(),
    startTime = session.startTime,
    endTime = session.endTime,
    date = session.date,
    appointmentSummaries = session.appointmentSummaries.map { appointmentSummary ->
      appointmentMappers.toDto(
        appointmentSummary,
        offenderInfoResults.first { it.crn == appointmentSummary.case.crn },
      )
    },
  )
}

fun ProjectSessionSummaries.toDto() = SessionSummariesDto(this.sessions.map { it.toDto() })
fun ProjectSessionSummary.toDto() = SessionSummaryDto(
  projectName = this.project.name,
  projectCode = this.project.code,
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  numberOfOffendersAllocated = this.allocatedCount,
  numberOfOffendersWithOutcomes = this.compliedOutcomeCount,
  numberOfOffendersWithEA = this.enforcementActionNeededCount,
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

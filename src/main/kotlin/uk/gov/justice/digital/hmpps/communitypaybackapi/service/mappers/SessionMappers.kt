package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import java.time.LocalTime

@Service
class SessionMappers(
  val appointmentMappers: AppointmentMappers,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun toDto(
    session: Session,
    offenderInfoResults: List<OffenderInfoResult>,
  ) = SessionDto(
    projectCode = session.project.code,
    projectName = session.project.name,
    projectLocation = session.project.location.toFullAddress(),
    location = session.project.location.toDto(),
    startTime = LocalTime.of(0, 0),
    endTime = LocalTime.of(0, 0),
    date = session.date,
    appointmentSummaries = session.appointmentSummaries.map { appointmentSummary ->
      appointmentMappers.toDto(
        appointmentSummary,
        offenderInfoResults.first { it.crn == appointmentSummary.case.crn },
      )
    },
  )

  fun toSummaryDto(
    session: Session,
  ) = SessionSummaryDto(
    projectName = session.project.name,
    projectCode = session.project.code,
    date = session.date,
    startTime = LocalTime.of(0, 0),
    endTime = LocalTime.of(0, 0),
    numberOfOffendersAllocated = session.appointmentSummaries.size,
    numberOfOffendersWithEA = session.appointmentSummaries.count { it.hasOutcome() && findOutcome(it.outcome!!.code).enforceable },
    numberOfOffendersWithOutcomes = session.appointmentSummaries.count { it.hasOutcome() },
  )

  private fun findOutcome(deliusCode: String) = contactOutcomeEntityRepository.findByCode(deliusCode) ?: error("Can't find outcome for code $deliusCode")
}

fun SessionSummaries.toDto() = SessionSummariesDto(this.sessions.map { it.toDto() })
fun SessionSummary.toDto() = SessionSummaryDto(
  projectName = this.project.name,
  projectCode = this.project.code,
  date = this.date,
  startTime = LocalTime.of(0, 0),
  endTime = LocalTime.of(0, 0),
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

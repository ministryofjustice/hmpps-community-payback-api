package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
class SessionMappers(
  val appointmentMappers: AppointmentMappers,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun toSessionDto(
    date: LocalDate,
    project: ProjectDto,
    appointments: List<AppointmentSummaryDto>,
  ) = SessionDto(
    projectCode = project.projectCode,
    projectName = project.projectName,
    location = project.location,
    date = date,
    appointmentSummaries = appointments,
    // deprecated fields
    projectLocation = "",
    startTime = LocalTime.of(0, 0),
    endTime = LocalTime.of(0, 0),
  )

  fun toSummaryDto(
    date: LocalDate,
    session: NDSession,
  ) = SessionSummaryDto(
    projectName = session.project.name,
    projectCode = session.project.code,
    date = date,
    startTime = LocalTime.of(0, 0),
    endTime = LocalTime.of(0, 0),
    numberOfOffendersAllocated = session.appointmentSummaries.size,
    numberOfOffendersWithEA = session.appointmentSummaries.count { it.hasOutcome() && findOutcome(it.outcome!!.code).enforceable },
    numberOfOffendersWithOutcomes = session.appointmentSummaries.count { it.hasOutcome() },
  )

  private fun findOutcome(deliusCode: String) = contactOutcomeEntityRepository.findByCode(deliusCode) ?: error("Can't find outcome for code $deliusCode")
}

fun NDSessionSummaries.toDto() = SessionSummariesDto(this.sessions.map { it.toDto() })
fun NDSessionSummary.toDto() = SessionSummaryDto(
  projectName = this.project.description,
  projectCode = this.project.code,
  date = this.date,
  startTime = LocalTime.of(0, 0),
  endTime = LocalTime.of(0, 0),
  numberOfOffendersAllocated = this.allocatedCount,
  numberOfOffendersWithOutcomes = this.outcomeCount,
  numberOfOffendersWithEA = this.enforcementActionCount,
)

fun NDAddress.toFullAddress() = listOfNotNull(
  this.buildingName,
  listOfNotNull(
    this.addressNumber,
    this.streetName,
  ).joinToString(" "),
  this.townCity,
  this.county,
  this.postCode,
).joinToString(", ")

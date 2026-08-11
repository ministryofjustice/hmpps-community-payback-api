package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto.OffenderFullDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PageMetaDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import java.time.LocalDate
import kotlin.comparisons.nullsLast

@Service
class SessionMappers(
  val appointmentMappers: AppointmentMappers,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  private val compareAppointmentsByPersonName: Comparator<AppointmentSummaryDto> = { a, b ->
    val offenderA = a.offender as? OffenderFullDto
    val offenderB = b.offender as? OffenderFullDto

    val comparator = nullsLast(compareBy<OffenderFullDto> { it.surname?.lowercase() }.thenBy { it.forename?.lowercase() })

    comparator.compare(offenderA, offenderB)
  }

  fun toSessionDto(
    date: LocalDate,
    project: ProjectDto,
    appointments: List<AppointmentSummaryDto>,
  ) = SessionDto(
    projectCode = project.projectCode,
    projectName = project.projectName,
    location = project.location,
    date = date,
    appointmentSummaries = appointments.sortedWith(compareAppointmentsByPersonName),
    // deprecated fields
    projectLocation = "",
  )

  fun toSummaryDto(
    date: LocalDate,
    project: ProjectDto,
    appointments: List<AppointmentSummaryDto>,
  ) = SessionSummaryDto(
    projectName = project.projectName,
    projectCode = project.projectCode,
    date = date,
    numberOfOffendersAllocated = appointments.size,
    numberOfOffendersWithEA = appointments.count { it.hasOutcome() && findOutcome(it.contactOutcome!!.code).enforceable },
    numberOfOffendersWithOutcomes = appointments.count { it.hasOutcome() },
  )

  private fun findOutcome(deliusCode: String) = contactOutcomeEntityRepository.findByCode(deliusCode) ?: error("Can't find outcome for code $deliusCode")
}

fun PageResponse.PageMeta.toDto() = PageMetaDto(
  size = size,
  number = number,
  totalElements = totalElements,
  totalPages = totalPages,
)

fun NDSessionSummary.toDto() = SessionSummaryDto(
  projectName = this.project.description,
  projectCode = this.project.code,
  date = this.date,
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

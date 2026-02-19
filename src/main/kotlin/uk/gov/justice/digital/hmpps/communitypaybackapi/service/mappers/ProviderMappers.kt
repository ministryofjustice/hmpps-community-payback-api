package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDGrade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.GradeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummaryDto

fun NDProviderSummaries.toDto() = ProviderSummariesDto(this.providers.map { it.toDto() })
fun NDProviderSummary.toDto() = ProviderSummaryDto(this.code, this.description)

fun NDProviderTeamSummaries.toDto() = ProviderTeamSummariesDto(this.teams.map { it.toDto() })
fun NDProviderTeamSummary.toDto() = ProviderTeamSummaryDto(this.code, this.description)

fun NDSupervisorSummaries.toDto() = SupervisorSummariesDto(
  this.supervisors
    .sortedBy { "${it.name.surname}${it.name.forename}".lowercase() }
    .map { it.toDto() },
)

fun NDSupervisorSummary.toDto() = SupervisorSummaryDto(
  code = this.code,
  name = NameDto(
    forename = this.name.forename,
    surname = this.name.surname,
    middleNames = this.name.middleName?.let { listOf(it) } ?: emptyList(),
  ),
  fullName = this.name.toDtoValue() + (this.grade?.let { " ${it.toDtoValue()}" } ?: ""),
  grade = this.grade?.toDto(),
  unallocated = this.unallocated,
)

fun NDGrade.toDto() = GradeDto(
  code = this.code,
  description = this.description,
)

fun NDSupervisorName.toDtoValue() = forename + " " +
  (middleName?.let { "$it " } ?: "") +
  surname

fun NDGrade.toDtoValue() = "[$code - $description]"

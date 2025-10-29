package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Grade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummaryDto

fun ProviderSummaries.toDto() = ProviderSummariesDto(this.providers.map { it.toDto() })
fun ProviderSummary.toDto() = ProviderSummaryDto(this.code, this.name)

fun ProviderTeamSummaries.toDto() = ProviderTeamSummariesDto(this.teams.map { it.toDto() })
fun ProviderTeamSummary.toDto() = ProviderTeamSummaryDto(this.code, this.description)

fun SupervisorSummaries.toDto() = SupervisorSummariesDto(
  this.supervisors
    .sortedBy { "${it.name.surname}${it.name.forename}".lowercase() }
    .map { it.toDto() },
)

fun SupervisorSummary.toDto() = SupervisorSummaryDto(
  this.code,
  this.name.toDtoValue() + (this.grade?.let { " ${it.toDtoValue()}" } ?: ""),
)

fun SupervisorName.toDtoValue() = forename + " " +
  (middleName?.let { "$it " } ?: "") +
  surname

fun Grade.toDtoValue() = "[$code - $description]"

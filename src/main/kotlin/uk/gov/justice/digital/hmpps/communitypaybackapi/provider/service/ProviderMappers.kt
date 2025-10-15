package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderTeamSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.SupervisorSummaryDto

fun ProviderSummaries.toDto() = ProviderSummariesDto(this.providers.map { it.toDto() })
fun ProviderSummary.toDto() = ProviderSummaryDto(this.id, this.code, this.name)

fun ProviderTeamSummaries.toDto() = ProviderTeamSummariesDto(this.teams.map { it.toDto() })
fun ProviderTeamSummary.toDto() = ProviderTeamSummaryDto(this.id, this.code, this.description)

fun SupervisorSummaries.toDto() = SupervisorSummariesDto(this.supervisors.map { it.toDto() })
fun SupervisorSummary.toDto() = SupervisorSummaryDto(
  this.officerCode,
  "${this.forename}${this.forename2?.let { " $it " } ?: " "}${this.surname} [${this.staffGrade}]",
)

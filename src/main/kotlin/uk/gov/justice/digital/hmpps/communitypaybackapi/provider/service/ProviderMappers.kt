package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderTeamSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.SupervisorSummaryDto

fun ProviderSummaries.toDto() = ProviderSummariesDto(this.providers.map { it.toDto() })
fun ProviderSummary.toDto() = ProviderSummaryDto(this.id, this.name)

fun ProviderTeamSummaries.toDto() = ProviderTeamSummariesDto(this.teams.map { it.toDto() })
fun ProviderTeamSummary.toDto() = ProviderTeamSummaryDto(this.id, this.name)

fun SupervisorSummaries.toDto() = SupervisorSummariesDto(this.supervisors.map { it.toDto() })
fun SupervisorSummary.toDto() = SupervisorSummaryDto(this.id, this.name)

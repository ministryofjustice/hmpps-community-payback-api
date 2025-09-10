package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummaryDto

fun ProviderSummaries.toDto() = ProviderSummariesDto(this.providers.map { it.toDto() })
fun ProviderSummary.toDto() = ProviderSummaryDto(this.id, this.name)

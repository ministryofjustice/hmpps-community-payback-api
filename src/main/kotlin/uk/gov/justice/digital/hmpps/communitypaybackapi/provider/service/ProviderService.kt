package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import org.springframework.stereotype.Service

@Suppress("MagicNumber")
val PROVIDER_SUMMARIES = ProviderSummaries(
  listOf(
    ProviderSummary(1000, "East of England"),
    ProviderSummary(2000, "North East Region"),
    ProviderSummary(3000, "North West Region"),
  ),
)

@Service
class ProviderService {
  fun getProviders() = PROVIDER_SUMMARIES.toDto()
}

data class ProviderSummaries(
  val providers: List<ProviderSummary>,
)

data class ProviderSummary(
  val id: Long,
  val name: String,
)

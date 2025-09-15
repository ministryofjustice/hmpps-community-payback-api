package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.provider.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service.toDto

class ProviderMappersTest {

  @Nested
  inner class ProviderSummaryMapper {

    @Test
    fun `should map using toDto() correctly`() {
      val providersSummaries = ProviderSummaries(
        listOf(
          ProviderSummary(1000, "East of England"),
          ProviderSummary(2000, "North East Region"),
          ProviderSummary(3000, "North West Region"),
        ),
      )
      val providerSummariesDto = providersSummaries.toDto()
      Assertions.assertThat(providerSummariesDto.providers).hasSize(3)
      Assertions.assertThat(providerSummariesDto.providers[0].id).isEqualTo(1000L)
      Assertions.assertThat(providerSummariesDto.providers[0].name).isEqualTo("East of England")
      Assertions.assertThat(providerSummariesDto.providers[1].id).isEqualTo(2000L)
      Assertions.assertThat(providerSummariesDto.providers[1].name).isEqualTo("North East Region")
      Assertions.assertThat(providerSummariesDto.providers[2].id).isEqualTo(3000L)
      Assertions.assertThat(providerSummariesDto.providers[2].name).isEqualTo("North West Region")
    }
  }

  @Nested
  inner class ProviderSummariesMapper {
    @Test
    fun `should map using toDto() correctly`() {
      val providerSummary = ProviderSummary(1000, "East of England")
      Assertions.assertThat(providerSummary.toDto()).isEqualTo(ProviderSummaryDto(1000, "East of England"))
    }
  }
}

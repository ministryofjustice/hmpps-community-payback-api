package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.provider.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.SupervisorSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service.toDto

class ProviderMappersTest {

  @Nested
  inner class ProviderSummaryMapper {

    @Test
    fun `should map using toDto() correctly`() {
      val providersSummaries = ProviderSummaries(
        listOf(
          ProviderSummary(1000, code = "ABC123", "East of England"),
          ProviderSummary(2000, code = "DEF123", "North East Region"),
          ProviderSummary(3000, code = "GHI123", "North West Region"),
        ),
      )
      val providerSummariesDto = providersSummaries.toDto()
      assertThat(providerSummariesDto.providers).hasSize(3)
      assertThat(providerSummariesDto.providers[0].id).isEqualTo(1000L)
      assertThat(providerSummariesDto.providers[0].name).isEqualTo("East of England")
      assertThat(providerSummariesDto.providers[1].id).isEqualTo(2000L)
      assertThat(providerSummariesDto.providers[1].name).isEqualTo("North East Region")
      assertThat(providerSummariesDto.providers[2].id).isEqualTo(3000L)
      assertThat(providerSummariesDto.providers[2].name).isEqualTo("North West Region")
    }
  }

  @Nested
  inner class ProviderSummariesMapper {
    @Test
    fun `should map using toDto() correctly`() {
      val providerSummary = ProviderSummary(1000, code = "GHI123", "East of England")
      assertThat(providerSummary.toDto()).isEqualTo(ProviderSummaryDto(1000, code = "GHI123", "East of England"))
    }
  }

  @Nested
  inner class SupervisorMappers {

    @Test
    fun `should map SupervisorSummaries to DTO correctly`() {
      val supervisorSummaries = SupervisorSummaries(
        listOf(
          SupervisorSummary(id = 4L, name = "Fred Flintstone"),
          SupervisorSummary(id = 5L, name = "Wilma Flintstone"),
          SupervisorSummary(id = 6L, name = "Barney Rubble"),
        ),
      )

      val supervisorSummariesDto = supervisorSummaries.toDto()

      assertThat(supervisorSummariesDto.supervisors).hasSize(3)
      assertThat(supervisorSummariesDto.supervisors[0].id).isEqualTo(4L)
      assertThat(supervisorSummariesDto.supervisors[0].name).isEqualTo("Fred Flintstone")
      assertThat(supervisorSummariesDto.supervisors[1].id).isEqualTo(5L)
      assertThat(supervisorSummariesDto.supervisors[1].name).isEqualTo("Wilma Flintstone")
      assertThat(supervisorSummariesDto.supervisors[2].id).isEqualTo(6L)
      assertThat(supervisorSummariesDto.supervisors[2].name).isEqualTo("Barney Rubble")
    }

    @Test
    fun `should map SupervisorSummary to DTO correctly`() {
      val supervisorSummary = SupervisorSummary(id = 4L, name = "Fred Flintstone")

      assertThat(supervisorSummary.toDto()).isEqualTo(
        SupervisorSummaryDto(id = 4L, name = "Fred Flintstone"),
      )
    }

    @Test
    fun `should map empty SupervisorSummaries list correctly`() {
      val supervisorSummaries = SupervisorSummaries(emptyList())
      val supervisorSummariesDto = supervisorSummaries.toDto()

      assertThat(supervisorSummariesDto.supervisors).isEmpty()
    }
  }
}

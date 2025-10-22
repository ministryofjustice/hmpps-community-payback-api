package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class ProviderMappersTest {

  @Nested
  inner class ProviderSummaryMapper {

    @Test
    fun `should map using toDto() correctly`() {
      val providersSummaries = ProviderSummaries(
        listOf(
          ProviderSummary(code = "ABC123", "East of England"),
          ProviderSummary(code = "DEF123", "North East Region"),
          ProviderSummary(code = "GHI123", "North West Region"),
        ),
      )
      val providerSummariesDto = providersSummaries.toDto()
      Assertions.assertThat(providerSummariesDto.providers).hasSize(3)
      Assertions.assertThat(providerSummariesDto.providers[0].name).isEqualTo("East of England")
      Assertions.assertThat(providerSummariesDto.providers[1].name).isEqualTo("North East Region")
      Assertions.assertThat(providerSummariesDto.providers[2].name).isEqualTo("North West Region")
    }
  }

  @Nested
  inner class ProviderSummariesMapper {
    @Test
    fun `should map using toDto() correctly`() {
      val providerSummary = ProviderSummary(code = "GHI123", name = "East of England")
      Assertions.assertThat(providerSummary.toDto())
        .isEqualTo(ProviderSummaryDto(code = "GHI123", name = "East of England"))
    }
  }

  @Nested
  inner class SupervisorMappers {

    @Test
    fun `should map SupervisorSummaries to DTO correctly`() {
      val supervisorSummaries = SupervisorSummaries(
        listOf(
          SupervisorSummary(
            forename = "Fred",
            forename2 = null,
            surname = "Flintstone",
            officerCode = "FF01",
            staffGrade = "PO",
          ),
          SupervisorSummary(
            forename = "Wilma",
            forename2 = null,
            surname = "Flintstone",
            officerCode = "WF01",
            staffGrade = "PO",
          ),
          SupervisorSummary(
            forename = "Barney",
            forename2 = null,
            surname = "Rubble",
            officerCode = "BR01",
            staffGrade = "PO",
          ),

        ),
      )

      val supervisorSummariesDto = supervisorSummaries.toDto()

      Assertions.assertThat(supervisorSummariesDto.supervisors).hasSize(3)
      Assertions.assertThat(supervisorSummariesDto.supervisors[0].code).isEqualTo("FF01")
      Assertions.assertThat(supervisorSummariesDto.supervisors[0].name).isEqualTo("Fred Flintstone [PO]")
      Assertions.assertThat(supervisorSummariesDto.supervisors[1].code).isEqualTo("WF01")
      Assertions.assertThat(supervisorSummariesDto.supervisors[1].name).isEqualTo("Wilma Flintstone [PO]")
      Assertions.assertThat(supervisorSummariesDto.supervisors[2].code).isEqualTo("BR01")
      Assertions.assertThat(supervisorSummariesDto.supervisors[2].name).isEqualTo("Barney Rubble [PO]")
    }

    @Test
    fun `should map SupervisorSummary to DTO correctly`() {
      val supervisorSummary = SupervisorSummary(
        forename = "Fred",
        forename2 = null,
        surname = "Flintstone",
        officerCode = "FF01",
        staffGrade = "PO",
      )

      Assertions.assertThat(supervisorSummary.toDto()).isEqualTo(
        SupervisorSummaryDto(code = "FF01", name = "Fred Flintstone [PO]"),
      )
    }

    @Test
    fun `should map empty SupervisorSummaries list correctly`() {
      val supervisorSummaries = SupervisorSummaries(emptyList())
      val supervisorSummariesDto = supervisorSummaries.toDto()

      Assertions.assertThat(supervisorSummariesDto.supervisors).isEmpty()
    }
  }
}

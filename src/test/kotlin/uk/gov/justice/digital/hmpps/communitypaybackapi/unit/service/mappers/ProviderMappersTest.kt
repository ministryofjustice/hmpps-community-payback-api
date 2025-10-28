package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Grade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorName
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
      assertThat(providerSummariesDto.providers).hasSize(3)
      assertThat(providerSummariesDto.providers[0].name).isEqualTo("East of England")
      assertThat(providerSummariesDto.providers[1].name).isEqualTo("North East Region")
      assertThat(providerSummariesDto.providers[2].name).isEqualTo("North West Region")
    }
  }

  @Nested
  inner class ProviderSummariesMapper {
    @Test
    fun `should map using toDto() correctly`() {
      val providerSummary = ProviderSummary(code = "GHI123", name = "East of England")
      assertThat(providerSummary.toDto())
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
            name = SupervisorName(
              forename = "Fred",
              middleName = null,
              surname = "Flintstone",
            ),
            code = "FF01",
            grade = Grade("PO", "PO Description"),
          ),
          SupervisorSummary(
            name = SupervisorName(
              forename = "Wilma",
              middleName = null,
              surname = "Flintstone",
            ),
            code = "WF01",
            grade = Grade("S1", "S1 Description"),
          ),
          SupervisorSummary(
            name = SupervisorName(
              forename = "Barney",
              middleName = null,
              surname = "Rubble",
            ),
            code = "BR01",
            grade = null,
          ),

        ),
      )

      val supervisorSummariesDto = supervisorSummaries.toDto()

      assertThat(supervisorSummariesDto.supervisors).hasSize(3)
      assertThat(supervisorSummariesDto.supervisors[0].code).isEqualTo("FF01")
      assertThat(supervisorSummariesDto.supervisors[0].name).isEqualTo("Fred Flintstone [PO - PO Description]")
      assertThat(supervisorSummariesDto.supervisors[1].code).isEqualTo("WF01")
      assertThat(supervisorSummariesDto.supervisors[1].name).isEqualTo("Wilma Flintstone [S1 - S1 Description]")
      assertThat(supervisorSummariesDto.supervisors[2].code).isEqualTo("BR01")
      assertThat(supervisorSummariesDto.supervisors[2].name).isEqualTo("Barney Rubble")
    }

    @Test
    fun `should map SupervisorSummary to DTO correctly`() {
      val supervisorSummary = SupervisorSummary(
        name = SupervisorName(
          forename = "Fred",
          middleName = null,
          surname = "Flintstone",
        ),
        code = "FF01",
        grade = Grade("PO", "PO Description"),
      )

      assertThat(supervisorSummary.toDto()).isEqualTo(
        SupervisorSummaryDto(code = "FF01", name = "Fred Flintstone [PO - PO Description]"),
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

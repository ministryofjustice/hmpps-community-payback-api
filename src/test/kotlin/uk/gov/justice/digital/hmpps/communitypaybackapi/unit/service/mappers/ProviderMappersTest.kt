package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDGrade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.GradeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class ProviderMappersTest {

  @Nested
  inner class ProviderSummaryMapper {

    @Test
    fun `should map using toDto() correctly`() {
      val providersSummaries = NDProviderSummaries(
        listOf(
          NDProviderSummary(code = "ABC123", "East of England"),
          NDProviderSummary(code = "DEF123", "North East Region"),
          NDProviderSummary(code = "GHI123", "North West Region"),
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
      val providerSummary = NDProviderSummary(code = "GHI123", description = "East of England")
      assertThat(providerSummary.toDto())
        .isEqualTo(ProviderSummaryDto(code = "GHI123", name = "East of England"))
    }
  }

  @Nested
  inner class SupervisorMappers {

    @Test
    fun `should map SupervisorSummaries to DTO correctly`() {
      val supervisorSummaries = NDSupervisorSummaries(
        listOf(
          NDSupervisorSummary(
            name = NDSupervisorName(
              forename = "wilma",
              middleName = null,
              surname = "flintstone",
            ),
            code = "WF01",
            grade = NDGrade("S1", "S1 Description"),
            unallocated = false,
          ),
          NDSupervisorSummary(
            name = NDSupervisorName(
              forename = "Barney",
              middleName = null,
              surname = "Rubble",
            ),
            code = "BR01",
            grade = null,
            unallocated = true,
          ),
          NDSupervisorSummary(
            name = NDSupervisorName(
              forename = "Fred",
              middleName = null,
              surname = "Flintstone",
            ),
            code = "FF01",
            grade = NDGrade("PO", "PO Description"),
            unallocated = false,
          ),
        ),
      )

      val supervisorSummariesDto = supervisorSummaries.toDto()

      assertThat(supervisorSummariesDto.supervisors).hasSize(3)
      assertThat(supervisorSummariesDto.supervisors[0].code).isEqualTo("FF01")
      assertThat(supervisorSummariesDto.supervisors[0].fullName).isEqualTo("Fred Flintstone [PO - PO Description]")
      assertThat(supervisorSummariesDto.supervisors[0].unallocated).isFalse
      assertThat(supervisorSummariesDto.supervisors[1].code).isEqualTo("WF01")
      assertThat(supervisorSummariesDto.supervisors[1].fullName).isEqualTo("wilma flintstone [S1 - S1 Description]")
      assertThat(supervisorSummariesDto.supervisors[1].unallocated).isFalse
      assertThat(supervisorSummariesDto.supervisors[2].code).isEqualTo("BR01")
      assertThat(supervisorSummariesDto.supervisors[2].fullName).isEqualTo("Barney Rubble")
      assertThat(supervisorSummariesDto.supervisors[2].unallocated).isTrue
    }

    @Test
    fun `should map SupervisorSummary to DTO correctly`() {
      val supervisorSummary = NDSupervisorSummary(
        name = NDSupervisorName(
          forename = "Fred",
          middleName = null,
          surname = "Flintstone",
        ),
        code = "FF01",
        grade = NDGrade("PO", "PO Description"),
        unallocated = false,
      )

      assertThat(supervisorSummary.toDto()).isEqualTo(
        SupervisorSummaryDto(
          code = "FF01",
          name = NameDto(
            forename = "Fred",
            surname = "Flintstone",
            middleNames = emptyList(),
          ),
          fullName = "Fred Flintstone [PO - PO Description]",
          grade = GradeDto(
            code = "PO",
            description = "PO Description",
          ),
          unallocated = false,
        ),
      )
    }

    @Test
    fun `should map empty SupervisorSummaries list correctly`() {
      val supervisorSummaries = NDSupervisorSummaries(emptyList())
      val supervisorSummariesDto = supervisorSummaries.toDto()

      assertThat(supervisorSummariesDto.supervisors).isEmpty()
    }
  }
}

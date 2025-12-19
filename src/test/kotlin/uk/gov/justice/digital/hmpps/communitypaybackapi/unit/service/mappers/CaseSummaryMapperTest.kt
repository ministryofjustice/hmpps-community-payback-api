package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

class CaseSummaryMapperTest {

  @Nested
  inner class CaseSummaryToOffenderDto {

    @Test
    fun `If no limitations return OffenderFullDto`() {
      val result = CaseSummary(
        crn = "CRN123",
        name = Name(
          forename = "thefore",
          surname = "thesur",
          middleNames = listOf("themid"),
        ),
        dateOfBirth = LocalDate.of(1983, 12, 11),
        currentExclusion = false,
        currentRestriction = false,
      ).toDto()

      assertThat(result).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      result as OffenderDto.OffenderFullDto

      assertThat(result.crn).isEqualTo("CRN123")
      assertThat(result.forename).isEqualTo("thefore")
      assertThat(result.surname).isEqualTo("thesur")
      assertThat(result.middleNames).contains("themid")
      assertThat(result.dateOfBirth).isEqualTo(LocalDate.of(1983, 12, 11))
    }

    @Test
    fun `If exclusion return OffenderLimitedDto`() {
      val result = CaseSummary.valid().copy(
        crn = "CRN123",
        currentExclusion = true,
        currentRestriction = false,
      ).toDto()

      assertThat(result).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      assertThat(result.crn).isEqualTo("CRN123")
    }

    @Test
    fun `If restriction return OffenderLimitedDto`() {
      val result = CaseSummary.valid().copy(
        crn = "CRN123",
        currentExclusion = false,
        currentRestriction = true,
      ).toDto()

      assertThat(result).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      assertThat(result.crn).isEqualTo("CRN123")
    }
  }
}

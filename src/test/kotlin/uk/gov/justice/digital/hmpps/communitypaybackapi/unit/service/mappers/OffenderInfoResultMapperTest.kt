package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

class OffenderInfoResultMapperTest {

  @Nested
  inner class OffenderInfoResultMapper {

    @Test
    fun `Full`() {
      val result = OffenderInfoResult.Full(
        crn = "CRN1",
        summary = CaseSummary(
          crn = "CRN1",
          name = Name(
            forename = "John",
            surname = "Smith",
            middleNames = listOf("J", "Jam"),
          ),
          dateOfBirth = LocalDate.of(1970, 2, 3),
        ),
      ).toDto()

      assertThat(result.crn).isEqualTo("CRN1")
      assertThat(result).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      result as OffenderDto.OffenderFullDto
      assertThat(result.forename).isEqualTo("John")
      assertThat(result.surname).isEqualTo("Smith")
      assertThat(result.middleNames).isEqualTo(listOf("J", "Jam"))
      assertThat(result.dateOfBirth).isEqualTo(LocalDate.of(1970, 2, 3))
    }

    @Test
    fun `Limited`() {
      val result = OffenderInfoResult.Limited("CRN2").toDto()

      assertThat(result.crn).isEqualTo("CRN2")
      assertThat(result).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
    }

    @Test
    fun `Not Found`() {
      val result = OffenderInfoResult.NotFound("CRN3").toDto()

      assertThat(result.crn).isEqualTo("CRN3")
      assertThat(result).isInstanceOf(OffenderDto.OffenderNotFoundDto::class.java)
    }
  }
}

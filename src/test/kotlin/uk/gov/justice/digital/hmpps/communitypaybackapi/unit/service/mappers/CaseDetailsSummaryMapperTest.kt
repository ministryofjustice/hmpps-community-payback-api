package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDMainOffence
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourtDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.MainOffenceDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

class CaseDetailsSummaryMapperTest {

  @Nested
  inner class NDCaseDetailToUnpaidWorkDetailsDto {

    @Test
    fun success() {
      val result = NDUpwDetails(
        eventNumber = 1,
        sentenceDate = LocalDate.of(2022, 2, 12),
        requiredMinutes = 1000,
        completedMinutes = 400,
        adjustments = 100,
        completedEteMinutes = 200,
        eventOutcome = "Event1",
        upwStatus = "UPW1",
        referralDate = LocalDate.of(2022, 2, 12),
        convictionDate = LocalDate.of(2022, 2, 12),
        mainOffence = NDMainOffence(
          date = LocalDate.of(2022, 2, 12),
          description = "Offence1",
          code = "12345",
          count = 1,
        ),
        court = NDCodeDescription(
          code = "12345",
          description = "Court1",
        ),
      ).toDto()

      assertThat(result.eventNumber).isEqualTo(1)
      assertThat(result.sentenceDate).isEqualTo(LocalDate.of(2022, 2, 12))
      assertThat(result.requiredMinutes).isEqualTo(1000)
      assertThat(result.adjustments).isEqualTo(100)
      assertThat(result.completedMinutes).isEqualTo(400)
      assertThat(result.allowedEteMinutes).isEqualTo(300)
      assertThat(result.completedEteMinutes).isEqualTo(200)
      assertThat(result.remainingEteMinutes).isEqualTo(100)
      assertThat(result.eventOutcome).isEqualTo("Event1")
      assertThat(result.upwStatus).isEqualTo("UPW1")
      assertThat(result.referralDate).isEqualTo(LocalDate.of(2022, 2, 12))
      assertThat(result.convictionDate).isEqualTo(LocalDate.of(2022, 2, 12))
      assertThat(result.mainOffence).isEqualTo(
        MainOffenceDto(
          date = LocalDate.of(2022, 2, 12),
          description = "Offence1",
          code = "12345",
          count = 1,
        ),
      )
      assertThat(result.court).isEqualTo(
        CourtDto(
          code = "12345",
          description = "Court1",
        ),
      )
    }
  }
}

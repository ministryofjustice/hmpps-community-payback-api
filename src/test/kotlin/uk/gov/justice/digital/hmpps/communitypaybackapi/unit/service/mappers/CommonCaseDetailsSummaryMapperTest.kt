package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDMainOffence
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourtDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.MainOffenceDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toCommonDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

class CommonCaseDetailsSummaryMapperTest {

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
        eventOutcomeCode = "OUTC",
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
        unpaidWorkRequirements = emptyList(),
      ).toDto().toCommonDto()

      assertThat(result.eventNumber).isEqualTo(1)
      assertThat(result.sentenceDate).isEqualTo(LocalDate.of(2022, 2, 12))
      assertThat(result.requiredMinutes).isEqualTo(1000)
      assertThat(result.totalAdjustmentMinutes).isEqualTo(100)
      assertThat(result.attendedAppointmentsMinutes).isEqualTo(400)
      assertThat(result.completedMinutes).isEqualTo(300)
      assertThat(result.remainingMinutes).isEqualTo(700)
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

    @ParameterizedTest
    @CsvSource("400, -60, 460, 540", "400, 0, 400, 600", "400, 100, 300, 700", "1000, -60, 1060, -60")
    fun `calculates completed and remaining minutes`(attended: Long, adjustments: Long, completed: Long, remaining: Long) {
      val result = NDUpwDetails.valid().copy(requiredMinutes = 1000, completedMinutes = attended, adjustments = adjustments).toDto().toCommonDto()

      assertThat(result.attendedAppointmentsMinutes).isEqualTo(attended)
      assertThat(result.totalAdjustmentMinutes).isEqualTo(adjustments)
      assertThat(result.completedMinutes).isEqualTo(completed)
      assertThat(result.remainingMinutes).isEqualTo(remaining)
    }

    @Test
    fun `maps correctly when court information is not provided`() {
      val result = NDUpwDetails.valid().copy(court = null).toDto().toCommonDto()

      assertThat(result.court).isNull()
    }

    @Test
    fun `maps correctly when conviction date is not provided`() {
      val result = NDUpwDetails.valid().copy(convictionDate = null).toDto().toCommonDto()

      assertThat(result.convictionDate).isNull()
    }
  }
}

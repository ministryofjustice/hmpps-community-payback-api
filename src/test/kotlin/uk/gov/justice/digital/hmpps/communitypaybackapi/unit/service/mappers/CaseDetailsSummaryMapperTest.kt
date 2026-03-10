package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

class CaseDetailsSummaryMapperTest {

  @Nested
  inner class NDCaseDetailToUnpaidWorkDetailsDto {

    @Test
    fun success() {
      val result = NDCaseDetail(
        eventNumber = 1,
        sentenceDate = LocalDate.of(2022, 2, 12),
        requiredMinutes = 1000,
        completedMinutes = 400,
        adjustments = 100,
        completedEteMinutes = 200,
      ).toDto()

      assertThat(result.eventNumber).isEqualTo(1)
      assertThat(result.sentenceDate).isEqualTo(LocalDate.of(2022, 2, 12))
      assertThat(result.requiredMinutes).isEqualTo(1000)
      assertThat(result.adjustments).isEqualTo(100)
      assertThat(result.completedMinutes).isEqualTo(400)
      assertThat(result.allowedEteMinutes).isEqualTo(300)
      assertThat(result.completedEteMinutes).isEqualTo(200)
      assertThat(result.remainingEteMinutes).isEqualTo(100)
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.throwValidationErrorForAppointmentUpdateCreate
import java.time.Duration

class AppointmentValidationHelpersTest {

  @ParameterizedTest
  @CsvSource("0, 0 hours 0 minutes", "90, 1 hours 30 minutes", "1500, 25 hours 0 minutes")
  fun `exceeding remaining ETE time throws a bad request with formatted durations`(remainingMinutes: Long, expectedRemainingTime: String) {
    val error = ValidationResultItem(
      field = "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]",
      code = "CREDITED_ETE_TIME_EXCEEDS_REMAINING_ETE_TIME",
      data = mapOf(
        "timeToCredit" to Duration.ofMinutes(remainingMinutes + 60),
        "remainingEteTime" to Duration.ofMinutes(remainingMinutes),
      ),
    )

    assertThatThrownBy { throwValidationErrorForAppointmentUpdateCreate(error) }
      .isInstanceOf(BadRequestException::class.java)
      .hasMessage("Credited minutes of '${remainingMinutes / 60 + 1} hours ${remainingMinutes % 60} minutes' exceeds remaining allowed ETE time of '$expectedRemainingTime'")
  }
}

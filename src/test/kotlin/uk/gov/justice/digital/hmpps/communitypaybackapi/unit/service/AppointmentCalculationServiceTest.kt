package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCalculationService
import java.time.Duration
import java.time.LocalTime

class AppointmentCalculationServiceTest {

  val service = AppointmentCalculationService()

  @Nested
  inner class MinutesToCredit {

    @Test
    fun `minutes credited is null if no outcome`() {
      val result = service.minutesToCredit(
        contactOutcome = null,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(12, 0),
        penaltyMinutes = null,
      )

      assertThat(result).isNull()
    }

    @Test
    fun `minutes credited is null if outcome indicates no attendance`() {
      val result = service.minutesToCredit(
        contactOutcome = ContactOutcomeEntity.valid().copy(attended = false),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(12, 0),
        penaltyMinutes = null,
      )

      assertThat(result).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "00:00,00:01,null,PT1M",
        "00:00,23:59,null,PT23H59M",
        "00:00,23:59,PT23H55M,PT4M",
        "10:00,11:00,null,PT1H",
        "10:00,11:00,PT59M,PT1M",
        "10:00,11:00,PT60M,null",
      ],
    )
    fun `minutes credited is added if outcome indicates attendance`(
      startTime: LocalTime,
      endTime: LocalTime,
      penaltyMinutes: Duration?,
      expectedTimeCredited: Duration?,
    ) {
      val result = service.minutesToCredit(
        contactOutcome = ContactOutcomeEntity.valid().copy(attended = true),
        startTime = startTime,
        endTime = endTime,
        penaltyMinutes = penaltyMinutes,
      )

      assertThat(result).isEqualTo(expectedTimeCredited)
    }
  }
}

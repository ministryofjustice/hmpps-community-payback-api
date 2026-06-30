package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class IncentiveSchemeEventTest {
  @Nested
  inner class IncentiveSchemeAppointmentEvent {
    @Nested
    inner class Timestamp {
      @Test
      fun `derived from appointment date and start time`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(date = LocalDate.of(2026, 6, 6), startTime = LocalTime.of(10, 0)),
        )

        assertThat(result.timestamp).isEqualTo(
          ZonedDateTime.of(2026, 6, 6, 10, 0, 0, 0, ZoneId.of("Europe/London")).toOffsetDateTime(),
        )
      }
    }

    @Nested
    inner class IsDisqualifying {
      @Test
      fun `is true when the contact outcome is enforceable`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = true)),
        )

        assertThat(result.isDisqualifying).isTrue
      }

      @Test
      fun `is false when the contact outcome is not enforceable`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = false)),
        )

        assertThat(result.isDisqualifying).isFalse
      }

      @Test
      fun `is false when there is no contact outcome`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = null),
        )

        assertThat(result.isDisqualifying).isFalse
      }
    }

    @Nested
    inner class IsQualifying {
      @Test
      fun `is true when there is a contact outcome`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid()),
        )

        assertThat(result.isQualifying).isTrue
      }

      @Test
      fun `is false when there is no contact outcome`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = null),
        )

        assertThat(result.isQualifying).isFalse
      }
    }

    @Nested
    inner class Duration {
      @Test
      fun `is equal to the number of minutes credited when present`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(minutesCredited = 120),
        )

        assertThat(result.duration).isEqualTo(java.time.Duration.ofMinutes(120))
      }

      @Test
      fun `is zero when the number of minutes credit is not present`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent(
          AppointmentSummaryDto.valid().copy(minutesCredited = null),
        )

        assertThat(result.duration).isEqualTo(java.time.Duration.ZERO)
      }
    }
  }

  @Nested
  inner class IncentiveSchemeCourseCompletionAppointmentEvent {
    @Nested
    inner class Timestamp {
      @Test
      fun `derived from appointment date and start time`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(
          AppointmentSummaryDto.valid().copy(date = LocalDate.of(2026, 6, 6), startTime = LocalTime.of(10, 0)),
        )

        assertThat(result.timestamp).isEqualTo(
          ZonedDateTime.of(2026, 6, 6, 10, 0, 0, 0, ZoneId.of("Europe/London")).toOffsetDateTime(),
        )
      }
    }

    @Nested
    inner class IsDisqualifying {
      @Test
      fun `is always false`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(AppointmentSummaryDto.valid())

        assertThat(result.isDisqualifying).isFalse
      }
    }

    @Nested
    inner class IsQualifying {
      @Test
      fun `is true when there is a contact outcome`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = ContactOutcomeDto.valid()),
        )

        assertThat(result.isQualifying).isTrue
      }

      @Test
      fun `is false when there is no contact outcome`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(
          AppointmentSummaryDto.valid().copy(contactOutcome = null),
        )

        assertThat(result.isQualifying).isFalse
      }
    }

    @Nested
    inner class Duration {
      @Test
      fun `is equal to the number of minutes credited when present`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(
          AppointmentSummaryDto.valid().copy(minutesCredited = 120),
        )

        assertThat(result.duration).isEqualTo(java.time.Duration.ofMinutes(120))
      }

      @Test
      fun `is zero when the number of minutes credit is not present`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent(
          AppointmentSummaryDto.valid().copy(minutesCredited = null),
        )

        assertThat(result.duration).isEqualTo(java.time.Duration.ZERO)
      }
    }
  }

  @Nested
  inner class IncentiveSchemeAdjustmentEvent {
    @Nested
    inner class Timestamp {
      @Test
      fun `is the date at the last second of the day`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent(
          AdjustmentDto.valid().copy(date = LocalDate.of(2026, 6, 6)),
        )

        assertThat(result.timestamp)
          .isEqualTo(ZonedDateTime.of(2026, 6, 6, 23, 59, 59, 0, ZoneId.of("Europe/London")).toOffsetDateTime())
      }
    }

    @Nested
    inner class IsDisqualifying {
      @Test
      fun `is always false`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent(AdjustmentDto.valid())

        assertThat(result.isDisqualifying).isFalse
      }
    }

    @Nested
    inner class IsQualifying {
      @Test
      fun `is always true`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent(AdjustmentDto.valid())

        assertThat(result.isQualifying).isTrue
      }
    }

    @Nested
    inner class Duration {
      @Test
      fun `is the negative of the amount added to the requirement`() {
        val result = IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent(
          // A negative adjustment of 20 minutes, i.e. 20 minutes removed from the requirement
          AdjustmentDto.valid().copy(amount = java.time.Duration.ofMinutes(-20)),
        )

        // A positive credit of 20 minutes
        assertThat(result.duration).isEqualTo(java.time.Duration.ofMinutes(20))
      }
    }
  }
}

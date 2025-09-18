package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.fromDto

class AppointmentMappersTest {

  @Nested
  inner class WorkQualityFromDto {
    @ParameterizedTest
    @CsvSource(
      "EXCELLENT,EXCELLENT",
      "GOOD,GOOD",
      "NOT_APPLICABLE,NOT_APPLICABLE",
      "POOR,POOR",
      "SATISFACTORY,SATISFACTORY",
      "UNSATISFACTORY,UNSATISFACTORY",
    )
    fun `all values mapped correctly`(
      sourceValue: AppointmentWorkQualityDto,
      expectedValue: WorkQuality,
    ) {
      assertThat(WorkQuality.fromDto(sourceValue)).isEqualTo(expectedValue)
    }
  }

  @Nested
  inner class BehaviourFromDto {
    @ParameterizedTest
    @CsvSource(
      "EXCELLENT,EXCELLENT",
      "GOOD,GOOD",
      "NOT_APPLICABLE,NOT_APPLICABLE",
      "POOR,POOR",
      "SATISFACTORY,SATISFACTORY",
      "UNSATISFACTORY,UNSATISFACTORY",
    )
    fun `all values mapped correctly`(
      sourceValue: AppointmentBehaviourDto,
      expectedValue: Behaviour,
    ) {
      assertThat(Behaviour.fromDto(sourceValue)).isEqualTo(expectedValue)
    }
  }
}

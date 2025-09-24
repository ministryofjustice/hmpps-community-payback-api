package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.fromDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AppointmentMappersTest {

  @Nested
  inner class ToAppointOutcomeDomainEventDetail {

    @Test
    fun success() {
      val appointmentOutcomeEntity = AppointmentOutcomeEntity.valid().copy(
        id = UUID.randomUUID(),
        appointmentDeliusId = 101L,
        projectTypeEntity = ProjectTypeEntity.valid().copy(code = "PT01"),
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcomeEntity = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorTeamDeliusId = 103L,
        supervisorOfficerDeliusId = 104L,
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
        enforcementActionEntity = EnforcementActionEntity.valid().copy(code = "EA01"),
        respondBy = LocalDate.of(2025, 1, 2),
      )

      val result = appointmentOutcomeEntity.toDomainEventDetail()

      assertThat(result.id).isEqualTo(appointmentOutcomeEntity.id)
      assertThat(result.appointmentDeliusId).isEqualTo(101L)
      assertThat(result.projectTypeDeliusCode).isEqualTo("PT01")
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.contactOutcomeDeliusCode).isEqualTo("COE1")
      assertThat(result.supervisorTeamDeliusId).isEqualTo(103L)
      assertThat(result.supervisorOfficerDeliusId).isEqualTo(104L)
      assertThat(result.notes).isEqualTo("The notes")
      assertThat(result.hiVisWorn).isTrue
      assertThat(result.workedIntensively).isFalse
      assertThat(result.penaltyMinutes).isEqualTo(105)
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviourDto.UNSATISFACTORY)
      assertThat(result.enforcementActionDeliusCode).isEqualTo("EA01")
      assertThat(result.respondBy).isEqualTo(LocalDate.of(2025, 1, 2))
    }
  }

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

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
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
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcomeEntity = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
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
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.contactOutcomeCode).isEqualTo("COE1")
      assertThat(result.supervisorOfficerCode).isEqualTo("WO3736")
      assertThat(result.notes).isEqualTo("The notes")
      assertThat(result.hiVisWorn).isTrue
      assertThat(result.workedIntensively).isFalse
      assertThat(result.penaltyMinutes).isEqualTo(105)
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviourDto.UNSATISFACTORY)
      assertThat(result.enforcementActionCode).isEqualTo("EA01")
      assertThat(result.respondBy).isEqualTo(LocalDate.of(2025, 1, 2))
    }
  }

  @Nested
  inner class ProjectAppointmentMapper {
    @Test
    fun `should map ProjectAppointment to DTO correctly`() {
      val id = 101L
      val projectName = "Community Garden Maintenance"
      val projectCode = "CGM101"
      val projectTypeName = "MAINTENANCE"
      val projectTypeCode = "MAINT"
      val crn = "CRN1"
      val contactOutcomeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
      val enforcementActionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
      val supervisingTeam = "Team Lincoln"
      val supervisingTeamCode = "TL01"
      val providerCode = "PC01"
      val date = LocalDate.of(2025, 9, 1)
      val startTime = LocalTime.of(9, 0)
      val endTime = LocalTime.of(17, 0)
      val penaltyTime = LocalTime.of(0, 0)
      val supervisorCode = "CRN1"
      val respondBy = LocalDate.of(2025, 10, 1)
      val hiVisWorn = true
      val workedIntensively = false
      val workQuality = ProjectAppointmentWorkQuality.SATISFACTORY
      val behaviour = ProjectAppointmentBehaviour.SATISFACTORY
      val notes = "This is a test note"
      val projectAppointment = ProjectAppointment(
        id = id,
        projectName = projectName,
        projectCode = projectCode,
        projectTypeName = projectTypeName,
        projectTypeCode = projectTypeCode,
        crn = crn,
        supervisingTeam = supervisingTeam,
        supervisingTeamCode = supervisingTeamCode,
        providerCode = providerCode,
        date = date,
        startTime = startTime,
        endTime = endTime,
        penaltyTime = penaltyTime,
        supervisorCode = supervisorCode,
        contactOutcomeId = contactOutcomeId,
        enforcementActionId = enforcementActionId,
        respondBy = respondBy,
        hiVisWorn = hiVisWorn,
        workedIntensively = workedIntensively,
        workQuality = workQuality,
        behaviour = behaviour,
        notes = notes,
      )

      val result = projectAppointment.toDto(OffenderInfoResult.Limited("CRN1"))

      assertThat(result.id).isEqualTo(id)
      assertThat(result.projectName).isEqualTo(projectName)
      assertThat(result.projectCode).isEqualTo(projectCode)
      assertThat(result.date).isEqualTo(date)
      assertThat(result.supervisingTeam).isEqualTo(supervisingTeam)
      assertThat(result.supervisingTeamCode).isEqualTo(supervisingTeamCode)
      assertThat(result.providerCode).isEqualTo(providerCode)
      assertThat(result.attendanceData?.supervisorOfficerCode).isEqualTo(supervisorCode)
      assertThat(result.attendanceData?.penaltyTime).isEqualTo(penaltyTime)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.SATISFACTORY)
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.SATISFACTORY)
      assertThat(result.attendanceData?.hiVisWorn).isEqualTo(hiVisWorn)
      assertThat(result.attendanceData?.contactOutcomeId).isEqualTo(contactOutcomeId)
      assertThat(result.enforcementData?.enforcementActionId).isEqualTo(enforcementActionId)
      assertThat(result.enforcementData?.respondBy).isEqualTo(respondBy)
      assertThat(result.notes).isEqualTo(notes)
      assertThat(result.offender.crn).isEqualTo(crn)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
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

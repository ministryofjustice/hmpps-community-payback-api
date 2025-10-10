package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentDraftEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

class AppointmentDraftMappersTest {

  @Nested
  inner class EntityToDto {

    @Test
    fun `maps all fields including nested data`() {
      val projectType = ProjectTypeEntity.valid().copy(code = "PTC", name = "Project Type")
      val contactOutcome = ContactOutcomeEntity.valid().copy(code = "ATTC", name = "Attended - Complied")
      val enforcement = EnforcementActionEntity.valid()

      val entity = AppointmentDraftEntity(
        id = UUID.randomUUID(),
        appointmentDeliusId = 999L,
        crn = "X12345",
        projectName = "Some Project",
        projectCode = "SP01",
        projectTypeId = projectType.id,
        projectTypeEntity = projectType,
        supervisingTeamCode = "TEAM1",
        appointmentDate = LocalDate.of(2025, 10, 10),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(14, 0),
        hiVisWorn = true,
        workedIntensively = false,
        penaltyTimeMinutes = 75L,
        workQuality = WorkQuality.GOOD,
        behaviour = Behaviour.POOR,
        supervisorOfficerCode = "SUP01",
        contactOutcomeId = contactOutcome.id,
        contactOutcomeEntity = contactOutcome,
        enforcementActionId = enforcement.id,
        enforcementActionEntity = enforcement,
        respondBy = LocalDate.of(2025, 10, 20),
        notes = "some notes",
        deliusLastUpdatedAt = OffsetDateTime.parse("2025-10-10T10:10:10Z"),
        createdAt = OffsetDateTime.parse("2025-10-10T11:11:11Z"),
        updatedAt = OffsetDateTime.parse("2025-10-10T12:12:12Z"),
      )

      val dto = entity.toDto()

      assertThat(dto.id).isEqualTo(entity.id)
      assertThat(dto.appointmentDeliusId).isEqualTo(999L)
      assertThat(dto.crn).isEqualTo("X12345")
      assertThat(dto.projectName).isEqualTo("Some Project")
      assertThat(dto.projectCode).isEqualTo("SP01")
      assertThat(dto.projectTypeId).isEqualTo(projectType.id)
      assertThat(dto.projectTypeCode).isEqualTo("PTC")
      assertThat(dto.projectTypeName).isEqualTo("Project Type")
      assertThat(dto.supervisingTeamCode).isEqualTo("TEAM1")
      assertThat(dto.appointmentDate).isEqualTo(LocalDate.of(2025, 10, 10))
      assertThat(dto.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(dto.endTime).isEqualTo(LocalTime.of(14, 0))
      assertThat(dto.notes).isEqualTo("some notes")
      assertThat(dto.deliusLastUpdatedAt).isEqualTo(OffsetDateTime.parse("2025-10-10T10:10:10Z"))
      assertThat(dto.createdAt).isEqualTo(OffsetDateTime.parse("2025-10-10T11:11:11Z"))
      assertThat(dto.updatedAt).isEqualTo(OffsetDateTime.parse("2025-10-10T12:12:12Z"))

      assertThat(dto.attendanceData).isNotNull
      val attendance = dto.attendanceData!!
      assertThat(attendance.hiVisWorn).isTrue
      assertThat(attendance.workedIntensively).isFalse
      assertThat(attendance.penaltyMinutes).isEqualTo(75L)
      assertThat(attendance.workQuality).isEqualTo(uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto.GOOD)
      assertThat(attendance.behaviour).isEqualTo(uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto.POOR)
      assertThat(attendance.supervisorOfficerCode).isEqualTo("SUP01")
      assertThat(attendance.contactOutcomeId).isEqualTo(contactOutcome.id)

      assertThat(dto.contactOutcome).isNotNull
      assertThat(dto.contactOutcome!!.id).isEqualTo(contactOutcome.id)
      assertThat(dto.contactOutcome!!.code).isEqualTo("ATTC")
      assertThat(dto.contactOutcome!!.name).isEqualTo("Attended - Complied")

      assertThat(dto.enforcementData).isNotNull
      assertThat(dto.enforcementData!!.enforcementActionId).isEqualTo(enforcement.id)
      assertThat(dto.enforcementData!!.respondBy).isEqualTo(LocalDate.of(2025, 10, 20))
    }

    @Test
    fun `null optional fields result in null nested dtos`() {
      val projectType = ProjectTypeEntity.valid()

      val entity = AppointmentDraftEntity(
        id = UUID.randomUUID(),
        appointmentDeliusId = 1000L,
        crn = "X1",
        projectName = "P",
        projectCode = "PC",
        projectTypeId = projectType.id,
        projectTypeEntity = projectType,
        supervisingTeamCode = null,
        appointmentDate = LocalDate.of(2025, 1, 1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        hiVisWorn = null,
        workedIntensively = null,
        penaltyTimeMinutes = null,
        workQuality = null,
        behaviour = null,
        supervisorOfficerCode = null,
        contactOutcomeId = null,
        contactOutcomeEntity = null,
        enforcementActionId = null,
        enforcementActionEntity = null,
        respondBy = null,
        notes = null,
        deliusLastUpdatedAt = OffsetDateTime.parse("2025-01-01T00:00:00Z"),
        createdAt = OffsetDateTime.parse("2025-01-01T00:00:01Z"),
        updatedAt = OffsetDateTime.parse("2025-01-01T00:00:02Z"),
      )

      val dto = entity.toDto()

      assertThat(dto.attendanceData).isNotNull
      assertThat(dto.attendanceData!!.penaltyMinutes).isNull()
      assertThat(dto.contactOutcome).isNull()
      assertThat(dto.enforcementData).isNull()
    }
  }
}

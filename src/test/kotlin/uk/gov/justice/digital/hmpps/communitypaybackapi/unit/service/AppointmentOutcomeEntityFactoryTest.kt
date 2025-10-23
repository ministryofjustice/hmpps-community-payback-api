package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentOutcomeEntityFactoryTest {

  @InjectMockKs
  lateinit var service: AppointmentOutcomeEntityFactory

  @Nested
  inner class ToEntity {

    @Test
    fun `to entity`() {
      val deliusVersion = UUID.randomUUID()

      val result = service.toEntity(
        101L,
        UpdateAppointmentOutcomeDto(
          deliusVersionToUpdate = deliusVersion,
          startTime = LocalTime.of(10, 1, 2),
          endTime = LocalTime.of(16, 3, 4),
          contactOutcomeId = UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"),
          supervisorOfficerCode = "N45",
          notes = "some notes",
          attendanceData = AttendanceDataDto(
            hiVisWorn = false,
            workedIntensively = true,
            penaltyTime = LocalTime.of(5, 0),
            workQuality = AppointmentWorkQualityDto.SATISFACTORY,
            behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
          ),
          enforcementData = EnforcementDto(
            enforcementActionId = UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"),
            respondBy = LocalDate.of(2026, 8, 10),
          ),
          formKeyToDelete = null,
          alertActive = false,
          sensitive = true,
        ),
      )

      assertThat(result.id).isNotNull
      assertThat(result.deliusVersionToUpdate).isEqualTo(deliusVersion)
      assertThat(result.appointmentDeliusId).isEqualTo(101L)
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(result.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
      assertThat(result.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))
      assertThat(result.supervisorOfficerCode).isEqualTo("N45")
      assertThat(result.notes).isEqualTo("some notes")
      assertThat(result.hiVisWorn).isEqualTo(false)
      assertThat(result.workedIntensively).isEqualTo(true)
      assertThat(result.penaltyMinutes).isEqualTo(300L)
      assertThat(result.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(result.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(result.respondBy).isEqualTo(LocalDate.of(2026, 8, 10))
      assertThat(result.alertActive).isEqualTo(false)
      assertThat(result.sensitive).isEqualTo(true)
    }
  }
}

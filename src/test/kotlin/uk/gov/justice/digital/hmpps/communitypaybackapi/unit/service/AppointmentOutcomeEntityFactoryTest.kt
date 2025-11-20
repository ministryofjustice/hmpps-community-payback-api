package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentOutcomeEntityFactoryTest {

  @MockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @InjectMockKs
  lateinit var service: AppointmentOutcomeEntityFactory

  companion object {
    const val CONTACT_OUTCOME_CODE: String = "CONTACT-1"
    val ENFORCEMENT_ACTION_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class ToEntity {

    @Test
    fun `to entity`() {
      val deliusVersion = UUID.randomUUID()

      val contactOutcomeEntity = ContactOutcomeEntity.valid().copy(code = CONTACT_OUTCOME_CODE)
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcomeEntity

      val result = service.toEntity(
        UpdateAppointmentOutcomeDto(
          deliusId = 101L,
          deliusVersionToUpdate = deliusVersion,
          startTime = LocalTime.of(10, 1, 2),
          endTime = LocalTime.of(16, 3, 4),
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
          supervisorOfficerCode = "N45",
          notes = "some notes",
          attendanceData = AttendanceDataDto(
            hiVisWorn = false,
            workedIntensively = true,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(300)),
            workQuality = AppointmentWorkQualityDto.SATISFACTORY,
            behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
          ),
          enforcementData = EnforcementDto(
            enforcementActionId = ENFORCEMENT_ACTION_ID,
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
      assertThat(result.contactOutcome).isEqualTo(contactOutcomeEntity)
      assertThat(result.supervisorOfficerCode).isEqualTo("N45")
      assertThat(result.notes).isEqualTo("some notes")
      assertThat(result.hiVisWorn).isEqualTo(false)
      assertThat(result.workedIntensively).isEqualTo(true)
      assertThat(result.penaltyMinutes).isEqualTo(300L)
      assertThat(result.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(result.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(result.alertActive).isEqualTo(false)
      assertThat(result.sensitive).isEqualTo(true)
    }

    @Test
    fun `to entity, mandatory fields only`() {
      val deliusVersion = UUID.randomUUID()

      val contactOutcomeEntity = ContactOutcomeEntity.valid().copy(code = CONTACT_OUTCOME_CODE)
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcomeEntity

      val result = service.toEntity(
        UpdateAppointmentOutcomeDto(
          deliusId = 101L,
          deliusVersionToUpdate = deliusVersion,
          startTime = LocalTime.of(10, 1, 2),
          endTime = LocalTime.of(16, 3, 4),
          contactOutcomeCode = null,
          supervisorOfficerCode = "N45",
          notes = null,
          attendanceData = null,
          enforcementData = null,
          formKeyToDelete = null,
          alertActive = null,
          sensitive = null,
        ),
      )

      assertThat(result.id).isNotNull
      assertThat(result.deliusVersionToUpdate).isEqualTo(deliusVersion)
      assertThat(result.appointmentDeliusId).isEqualTo(101L)
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(result.contactOutcome).isNull()
      assertThat(result.supervisorOfficerCode).isEqualTo("N45")
      assertThat(result.notes).isNull()
      assertThat(result.hiVisWorn).isNull()
      assertThat(result.workedIntensively).isNull()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.workQuality).isNull()
      assertThat(result.behaviour).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
    }
  }
}

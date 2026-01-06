package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
    fun `all fields populated`() {
      val deliusVersion = UUID.randomUUID()

      val contactOutcomeEntity = ContactOutcomeEntity.valid().copy(
        code = CONTACT_OUTCOME_CODE,
        attended = true,
      )

      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcomeEntity

      val result = service.toEntity(
        UpdateAppointmentOutcomeDto(
          deliusId = 101L,
          deliusVersionToUpdate = deliusVersion,
          startTime = LocalTime.of(10, 1),
          endTime = LocalTime.of(16, 3),
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
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3))
      assertThat(result.contactOutcome).isEqualTo(contactOutcomeEntity)
      assertThat(result.supervisorOfficerCode).isEqualTo("N45")
      assertThat(result.notes).isEqualTo("some notes")
      assertThat(result.hiVisWorn).isEqualTo(false)
      assertThat(result.workedIntensively).isEqualTo(true)
      assertThat(result.penaltyMinutes).isEqualTo(300L)
      assertThat(result.minutesCredited).isEqualTo(62L)
      assertThat(result.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(result.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(result.alertActive).isEqualTo(false)
      assertThat(result.sensitive).isEqualTo(true)
    }

    @Test
    fun `mandatory fields only`() {
      val deliusVersion = UUID.randomUUID()

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
      assertThat(result.minutesCredited).isNull()
      assertThat(result.workQuality).isNull()
      assertThat(result.behaviour).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
    }

    @Test
    fun `minutes credited is null if no outcome`() {
      val result = service.toEntity(
        UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
        ),
      )

      assertThat(result.minutesCredited).isNull()
    }

    @Test
    fun `minutes credited is null if outcome indicates no attendance`() {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = false)

      val result = service.toEntity(
        UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(12, 0),
          attendanceData = AttendanceDataDto.valid().copy(penaltyTime = null),
        ),
      )

      assertThat(result.minutesCredited).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "00:00,00:01,null,1",
        "00:00,23:59,null,1439",
        "00:00,23:59,PT23H55M,4",
        "10:00,11:00,null,60",
        "10:00,11:00,PT59M,1",
        "10:00,11:00,PT60M,null",
      ],
    )
    fun `minutes credited is added if outcome indicates attendance`(
      startTime: LocalTime,
      endTime: LocalTime,
      penaltyTime: Duration?,
      expectedTimeCredited: Long?,
    ) {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = true)

      val result = service.toEntity(
        UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
          startTime = startTime,
          endTime = endTime,
          attendanceData = AttendanceDataDto.valid().copy(penaltyTime = penaltyTime?.let { HourMinuteDuration(it) }),
        ),
      )

      assertThat(result.minutesCredited).isEqualTo(expectedTimeCredited)
    }
  }
}

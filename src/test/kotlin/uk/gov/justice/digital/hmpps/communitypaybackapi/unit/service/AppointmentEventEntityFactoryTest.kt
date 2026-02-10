package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentEventEntityFactoryTest {

  @RelaxedMockK
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @RelaxedMockK
  lateinit var projectService: ProjectService

  @InjectMockKs
  lateinit var factory: AppointmentEventEntityFactory

  companion object {
    const val CONTACT_OUTCOME_CODE: String = "CONTACT-1"
    val ENFORCEMENT_ACTION_ID: UUID = UUID.randomUUID()
    val TRIGGERED_AT: OffsetDateTime = OffsetDateTime.now()
    const val TRIGGERED_BY: String = "User1"
    val ID: UUID = UUID.randomUUID()
    const val PROJECT_CODE: String = "PC01"
    val PROJECT = ProjectDto.valid().copy(
      projectCode = PROJECT_CODE,
      projectName = "The project name",
    )
  }

  @Nested
  inner class BuildCreatedEvent {

    @BeforeEach
    fun `setup get project mock`() {
      every { projectService.getProject(PROJECT_CODE) } returns PROJECT
    }

    @Test
    fun `all fields populated`() {
      val contactOutcomeEntity = ContactOutcomeEntity.valid().copy(
        code = CONTACT_OUTCOME_CODE,
        attended = true,
      )
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcomeEntity

      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto(
          id = ID,
          crn = "X12345",
          deliusEventNumber = 48,
          allocationId = 22,
          date = LocalDate.of(2014, 6, 7),
          startTime = LocalTime.of(10, 1),
          endTime = LocalTime.of(16, 3),
          pickUpLocationCode = "PICKUPLOC1",
          pickUpLocationDescription = "Pickup Description",
          pickUpTime = LocalTime.of(20, 5),
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
          supervisorOfficerCode = "N45",
          notes = "some notes",
          attendanceData = AttendanceDataDto(
            hiVisWorn = false,
            workedIntensively = true,
            penaltyMinutes = 300,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(400)),
            workQuality = AppointmentWorkQualityDto.SATISFACTORY,
            behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
          ),
          alertActive = false,
          sensitive = true,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.id).isNotNull
      assertThat(result.communityPaybackAppointmentId).isEqualTo(ID)
      assertThat(result.eventType).isEqualTo(AppointmentEventType.CREATE)
      assertThat(result.priorDeliusVersion).isNull()
      assertThat(result.crn).isEqualTo("X12345")
      assertThat(result.deliusEventNumber).isEqualTo(48)
      assertThat(result.projectCode).isEqualTo(PROJECT_CODE)
      assertThat(result.projectName).isEqualTo("The project name")
      assertThat(result.deliusAppointmentId).isEqualTo(101L)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 6, 7))
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3))
      assertThat(result.pickupLocationCode).isEqualTo("PICKUPLOC1")
      assertThat(result.pickupLocationDescription).isEqualTo("Pickup Description")
      assertThat(result.pickupTime).isEqualTo(LocalTime.of(20, 5))
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
      assertThat(result.deliusAllocationId).isEqualTo(22)
      assertThat(result.triggeredAt).isEqualTo(TRIGGERED_AT)
      assertThat(result.triggerType).isEqualTo(AppointmentEventTriggerType.SCHEDULING)
      assertThat(result.triggeredBy).isEqualTo(TRIGGERED_BY)
    }

    @Test
    fun `mandatory fields only`() {
      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto(
          id = ID,
          crn = "X12345",
          deliusEventNumber = 48,
          allocationId = null,
          date = LocalDate.of(2014, 6, 7),
          startTime = LocalTime.of(10, 1),
          endTime = LocalTime.of(16, 3),
          pickUpLocationCode = null,
          pickUpLocationDescription = null,
          pickUpTime = null,
          contactOutcomeCode = null,
          supervisorOfficerCode = null,
          notes = null,
          attendanceData = null,
          alertActive = null,
          sensitive = null,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.id).isNotNull
      assertThat(result.communityPaybackAppointmentId).isEqualTo(ID)
      assertThat(result.eventType).isEqualTo(AppointmentEventType.CREATE)
      assertThat(result.priorDeliusVersion).isNull()
      assertThat(result.crn).isEqualTo("X12345")
      assertThat(result.deliusEventNumber).isEqualTo(48)
      assertThat(result.projectCode).isEqualTo(PROJECT_CODE)
      assertThat(result.projectName).isEqualTo("The project name")
      assertThat(result.deliusAppointmentId).isEqualTo(101L)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 6, 7))
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3))
      assertThat(result.pickupLocationCode).isNull()
      assertThat(result.pickupLocationDescription).isNull()
      assertThat(result.pickupTime).isNull()
      assertThat(result.contactOutcome).isNull()
      assertThat(result.supervisorOfficerCode).isNull()
      assertThat(result.notes).isNull()
      assertThat(result.hiVisWorn).isNull()
      assertThat(result.workedIntensively).isNull()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.minutesCredited).isNull()
      assertThat(result.workQuality).isNull()
      assertThat(result.behaviour).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
      assertThat(result.deliusAllocationId).isNull()
      assertThat(result.triggeredAt).isEqualTo(TRIGGERED_AT)
      assertThat(result.triggerType).isEqualTo(AppointmentEventTriggerType.SCHEDULING)
      assertThat(result.triggeredBy).isEqualTo(TRIGGERED_BY)
    }

    @Test
    fun `minutes credited is null if no outcome`() {
      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto.valid().copy(
          contactOutcomeCode = null,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isNull()
    }

    @Test
    fun `minutes credited is null if outcome indicates no attendance`() {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = false)

      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto.valid().copy(
          contactOutcomeCode = null,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(12, 0),
          attendanceData = AttendanceDataDto.valid().copy(penaltyTime = null),
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "00:00,00:01,null,null,1",
        "00:00,23:59,null,null,1439",
        "00:00,23:59,PT23H55M,null,4",
        "00:00,23:59,null,PT23H55M,4",
        "00:00,23:59,PT1H,PT23H55M,4",
        "10:00,11:00,null,null,60",
        "10:00,11:00,PT59M,null,1",
        "10:00,11:00,null,PT59M,1",
        "10:00,11:00,PT60M,null,null",
        "10:00,11:00,null,PT60M,null",
      ],
    )
    fun `minutes credited is added if outcome indicates attendance`(
      startTime: LocalTime,
      endTime: LocalTime,
      penaltyTime: Duration?,
      penaltyMinutes: Duration?,
      expectedTimeCredited: Long?,
    ) {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = true)

      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto.valid().copy(
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
          startTime = startTime,
          endTime = endTime,
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = penaltyMinutes?.toMinutes(),
            penaltyTime = penaltyTime?.let { HourMinuteDuration(it) },
          ),
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isEqualTo(expectedTimeCredited)
    }

    @Test
    fun `use penaltyMinutes instead of penaltyTime if defined`() {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid()

      val result = factory.buildCreatedEvent(
        deliusId = 101L,
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.SCHEDULING,
          triggeredBy = TRIGGERED_BY,
        ),
        createAppointmentDto = CreateAppointmentDto.valid().copy(
          contactOutcomeCode = null,
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 150,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(300)),
          ),
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.penaltyMinutes).isEqualTo(150L)
    }
  }

  @Nested
  inner class BuildUpdatedEvent {

    @BeforeEach
    fun `setup get project mock`() {
      every { projectService.getProject(PROJECT_CODE) } returns PROJECT
    }

    @Test
    fun `all fields populated`() {
      val communityPaybackId = UUID.randomUUID()
      val deliusVersion = UUID.randomUUID()

      val contactOutcomeEntity = ContactOutcomeEntity.valid().copy(
        code = CONTACT_OUTCOME_CODE,
        attended = true,
      )

      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns contactOutcomeEntity

      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto(
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
            penaltyMinutes = 300,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(400)),
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
        existingAppointment = AppointmentDto.valid().copy(
          communityPaybackId = communityPaybackId,
          offender = OffenderDto.OffenderLimitedDto(crn = "X12345"),
          deliusEventNumber = 48,
          projectCode = "PC01",
          projectName = "The project name",
          date = LocalDate.of(2014, 6, 7),
          pickUpData = PickUpDataDto.valid().copy(
            locationCode = "PICKUP99",
            time = LocalTime.of(5, 45),
          ),
        ),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.id).isNotNull
      assertThat(result.communityPaybackAppointmentId).isEqualTo(communityPaybackId)
      assertThat(result.eventType).isEqualTo(AppointmentEventType.UPDATE)
      assertThat(result.priorDeliusVersion).isEqualTo(deliusVersion)
      assertThat(result.crn).isEqualTo("X12345")
      assertThat(result.deliusEventNumber).isEqualTo(48)
      assertThat(result.projectCode).isEqualTo(PROJECT_CODE)
      assertThat(result.projectName).isEqualTo("The project name")
      assertThat(result.deliusAppointmentId).isEqualTo(101L)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 6, 7))
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3))
      assertThat(result.pickupLocationCode).isEqualTo("PICKUP99")
      assertThat(result.pickupTime).isEqualTo(LocalTime.of(5, 45))
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
      assertThat(result.deliusAllocationId).isNull()
      assertThat(result.triggeredAt).isEqualTo(TRIGGERED_AT)
      assertThat(result.triggerType).isEqualTo(AppointmentEventTriggerType.USER)
      assertThat(result.triggeredBy).isEqualTo(TRIGGERED_BY)
    }

    @Test
    fun `mandatory fields only`() {
      val deliusVersion = UUID.randomUUID()

      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto(
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
        existingAppointment = AppointmentDto.valid().copy(
          offender = OffenderDto.OffenderLimitedDto(crn = "X12345"),
          communityPaybackId = null,
          deliusEventNumber = 48,
          projectCode = "PC01",
          projectName = "The project name",
          date = LocalDate.of(2014, 6, 7),
          pickUpData = null,
        ),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.id).isNotNull
      assertThat(result.communityPaybackAppointmentId).isNull()
      assertThat(result.eventType).isEqualTo(AppointmentEventType.UPDATE)
      assertThat(result.priorDeliusVersion).isEqualTo(deliusVersion)
      assertThat(result.crn).isEqualTo("X12345")
      assertThat(result.deliusEventNumber).isEqualTo(48)
      assertThat(result.projectCode).isEqualTo(PROJECT_CODE)
      assertThat(result.projectName).isEqualTo("The project name")
      assertThat(result.deliusAppointmentId).isEqualTo(101L)
      assertThat(result.date).isEqualTo(LocalDate.of(2014, 6, 7))
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(result.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(result.pickupLocationCode).isNull()
      assertThat(result.pickupTime).isNull()
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
      assertThat(result.deliusAllocationId).isNull()
      assertThat(result.triggeredAt).isEqualTo(TRIGGERED_AT)
      assertThat(result.triggerType).isEqualTo(AppointmentEventTriggerType.USER)
      assertThat(result.triggeredBy).isEqualTo(TRIGGERED_BY)
    }

    @Test
    fun `minutes credited is null if no outcome`() {
      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
        ),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isNull()
    }

    @Test
    fun `minutes credited is null if outcome indicates no attendance`() {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = false)

      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(12, 0),
          attendanceData = AttendanceDataDto.valid().copy(penaltyTime = null),
        ),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isNull()
    }

    @ParameterizedTest
    @CsvSource(
      nullValues = ["null"],
      value = [
        "00:00,00:01,null,null,1",
        "00:00,23:59,null,null,1439",
        "00:00,23:59,PT23H55M,null,4",
        "00:00,23:59,null,PT23H55M,4",
        "00:00,23:59,PT1H,PT23H55M,4",
        "10:00,11:00,null,null,60",
        "10:00,11:00,PT59M,null,1",
        "10:00,11:00,null,PT59M,1",
        "10:00,11:00,PT60M,null,null",
        "10:00,11:00,null,PT60M,null",
      ],
    )
    fun `minutes credited is added if outcome indicates attendance`(
      startTime: LocalTime,
      endTime: LocalTime,
      penaltyTime: Duration?,
      penaltyMinutes: Duration?,
      expectedTimeCredited: Long?,
    ) {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid().copy(attended = true)

      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = CONTACT_OUTCOME_CODE,
          startTime = startTime,
          endTime = endTime,
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = penaltyMinutes?.toMinutes(),
            penaltyTime = penaltyTime?.let { HourMinuteDuration(it) },
          ),
        ),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.minutesCredited).isEqualTo(expectedTimeCredited)
    }

    @Test
    fun `use penaltyMinutes instead of penaltyTime if defined`() {
      every { contactOutcomeEntityRepository.findByCode(CONTACT_OUTCOME_CODE) } returns ContactOutcomeEntity.valid()

      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = 150,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(300)),
          ),
        ),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.penaltyMinutes).isEqualTo(150L)
    }

    @Test
    fun `use legacy penaltyTime if penaltyMinutes not defined`() {
      val result = factory.buildUpdatedEvent(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          contactOutcomeCode = null,
          attendanceData = AttendanceDataDto.valid().copy(
            penaltyMinutes = null,
            penaltyTime = HourMinuteDuration(Duration.ofMinutes(300)),
          ),
        ),
        existingAppointment = AppointmentDto.valid(),
        trigger = AppointmentEventTrigger(
          triggeredAt = TRIGGERED_AT,
          triggerType = AppointmentEventTriggerType.USER,
          triggeredBy = TRIGGERED_BY,
        ),
        projectCode = PROJECT_CODE,
      )

      assertThat(result.penaltyMinutes).isEqualTo(300L)
    }
  }
}

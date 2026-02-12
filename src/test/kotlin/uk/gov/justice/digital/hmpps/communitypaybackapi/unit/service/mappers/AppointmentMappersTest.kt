package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProvider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAppointmentUpdatedDomainEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDUpdateAppointment
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentMappersTest {

  @MockK(relaxed = true)
  private lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @MockK(relaxed = true)
  private lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @InjectMockKs
  private lateinit var service: AppointmentMappers

  @Nested
  inner class AppointmentEventEntityToNDCreateAppointment {

    @Test
    fun success() {
      val appointmentId = UUID.randomUUID()

      val event = AppointmentEventEntity.valid().copy(
        id = UUID.randomUUID(),
        communityPaybackAppointmentId = appointmentId,
        eventType = AppointmentEventType.CREATE,
        deliusAppointmentId = 101L,
        priorDeliusVersion = UUID.randomUUID(),
        crn = "CRN123",
        deliusEventNumber = 5,
        date = LocalDate.of(2028, 7, 6),
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        pickupLocationCode = "PICKUP10",
        pickupTime = LocalTime.of(13, 14, 15),
        contactOutcome = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        minutesCredited = 35,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
        alertActive = false,
        sensitive = true,
      )

      val result = event.toNDCreateAppointment()

      assertThat(result.reference).isEqualTo(appointmentId)
      assertThat(result.crn).isEqualTo("CRN123")
      assertThat(result.eventNumber).isEqualTo(5)
      assertThat(result.date).isEqualTo(LocalDate.of(2028, 7, 6))
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome?.code).isEqualTo("COE1")
      assertThat(result.supervisor?.code).isEqualTo("WO3736")
      assertThat(result.notes).isEqualTo("The notes")
      assertThat(result.hiVisWorn).isTrue
      assertThat(result.workedIntensively).isFalse
      assertThat(result.penaltyMinutes).isEqualTo(105)
      assertThat(result.minutesCredited).isEqualTo(35)
      assertThat(result.workQuality).isEqualTo(NDAppointmentWorkQuality.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(NDAppointmentBehaviour.UNSATISFACTORY)
      assertThat(result.alertActive).isFalse
      assertThat(result.sensitive).isTrue
      assertThat(result.pickUp?.location?.code).isEqualTo("PICKUP10")
      assertThat(result.pickUp?.time).isEqualTo(LocalTime.of(13, 14, 15))
    }

    @Test
    fun `success with only mandatory fields`() {
      val appointmentId = UUID.randomUUID()

      val event = AppointmentEventEntity.valid().copy(
        id = UUID.randomUUID(),
        communityPaybackAppointmentId = appointmentId,
        eventType = AppointmentEventType.CREATE,
        deliusAppointmentId = 101L,
        priorDeliusVersion = UUID.randomUUID(),
        crn = "CRN123",
        deliusEventNumber = 5,
        date = LocalDate.of(2028, 7, 6),
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        pickupTime = null,
        pickupLocationCode = null,
        contactOutcome = null,
        supervisorOfficerCode = null,
        notes = null,
        hiVisWorn = null,
        workedIntensively = null,
        penaltyMinutes = null,
        minutesCredited = null,
        workQuality = null,
        behaviour = null,
        alertActive = null,
        sensitive = null,
      )

      val result = event.toNDCreateAppointment()

      assertThat(result.reference).isEqualTo(appointmentId)
      assertThat(result.crn).isEqualTo("CRN123")
      assertThat(result.eventNumber).isEqualTo(5)
      assertThat(result.date).isEqualTo(LocalDate.of(2028, 7, 6))
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome).isNull()
      assertThat(result.supervisor).isNull()
      assertThat(result.notes).isNull()
      assertThat(result.hiVisWorn).isNull()
      assertThat(result.workedIntensively).isNull()
      assertThat(result.penaltyMinutes).isNull()
      assertThat(result.minutesCredited).isNull()
      assertThat(result.workQuality).isNull()
      assertThat(result.behaviour).isNull()
      assertThat(result.alertActive).isNull()
      assertThat(result.sensitive).isNull()
      assertThat(result.pickUp?.location).isNull()
      assertThat(result.pickUp?.time).isNull()
    }
  }

  @Nested
  inner class AppointmentEventEntityToUpdateAppointment {

    @Test
    fun success() {
      val appointmentEvent = AppointmentEventEntity.valid().copy(
        id = UUID.randomUUID(),
        eventType = AppointmentEventType.UPDATE,
        deliusAppointmentId = 101L,
        priorDeliusVersion = UUID.randomUUID(),
        crn = "CRN123",
        deliusEventNumber = 5,
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        minutesCredited = 35,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
        alertActive = false,
        sensitive = true,
      )

      val result = appointmentEvent.toNDUpdateAppointment()

      assertThat(result.version).isEqualTo(appointmentEvent.priorDeliusVersion)
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome!!.code).isEqualTo("COE1")
      assertThat(result.supervisor.code).isEqualTo("WO3736")
      assertThat(result.notes).isEqualTo("The notes")
      assertThat(result.hiVisWorn).isTrue
      assertThat(result.workedIntensively).isFalse
      assertThat(result.penaltyMinutes).isEqualTo(105)
      assertThat(result.minutesCredited).isEqualTo(35)
      assertThat(result.workQuality).isEqualTo(NDAppointmentWorkQuality.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(NDAppointmentBehaviour.UNSATISFACTORY)
      assertThat(result.alertActive).isFalse
      assertThat(result.sensitive).isTrue
    }

    @Test
    fun `success with only mandatory fields`() {
      val event = AppointmentEventEntity.valid().copy(
        id = UUID.randomUUID(),
        eventType = AppointmentEventType.UPDATE,
        deliusAppointmentId = 101L,
        priorDeliusVersion = UUID.randomUUID(),
        crn = "CRN123",
        deliusEventNumber = 5,
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = null,
        supervisorOfficerCode = "WO3736",
        notes = null,
        hiVisWorn = null,
        workedIntensively = null,
        penaltyMinutes = null,
        minutesCredited = null,
        workQuality = null,
        behaviour = null,
        alertActive = null,
        sensitive = null,
      )

      val result = event.toNDUpdateAppointment()

      assertThat(result.version).isEqualTo(event.priorDeliusVersion)
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome).isNull()
      assertThat(result.supervisor.code).isEqualTo("WO3736")
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
  }

  @Nested
  inner class AppointmentEventEntityToUpdateDomainEventDetail {

    @Test
    fun success() {
      val appointmentEvent = AppointmentEventEntity.valid().copy(
        id = UUID.randomUUID(),
        deliusAppointmentId = 101L,
        crn = "CRN123",
        deliusEventNumber = 52,
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        minutesCredited = 55,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
      )

      val result = appointmentEvent.toAppointmentUpdatedDomainEvent()

      val detail = result.appointment
      assertThat(detail.id).isEqualTo(appointmentEvent.id)
      assertThat(detail.crn).isEqualTo("CRN123")
      assertThat(detail.deliusEventNumber).isEqualTo(52)
      assertThat(detail.appointmentDeliusId).isEqualTo(101L)
      assertThat(detail.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(detail.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(detail.contactOutcomeCode).isEqualTo("COE1")
      assertThat(detail.supervisorOfficerCode).isEqualTo("WO3736")
      assertThat(detail.notes).isEqualTo("The notes")
      assertThat(detail.hiVisWorn).isTrue
      assertThat(detail.workedIntensively).isFalse
      assertThat(detail.penaltyMinutes).isEqualTo(105)
      assertThat(detail.minutesCredited).isEqualTo(55)
      assertThat(detail.workQuality).isEqualTo(AppointmentWorkQualityDto.NOT_APPLICABLE)
      assertThat(detail.behaviour).isEqualTo(AppointmentBehaviourDto.UNSATISFACTORY)
    }
  }

  @Nested
  inner class AppointmentToDtoMapper {
    @Test
    fun `should map ProjectAppointment to DTO correctly`() {
      val id = 101L
      val communityPaybackId = UUID.randomUUID()
      val version = UUID.randomUUID()
      val projectName = "Community Garden Maintenance"
      val projectCode = "CGM101"
      val projectTypeName = "MAINTENANCE"
      val projectTypeCode = "MAINT"
      val crn = "CRN1"
      val eventNumber = 98
      val contactOutcomeCode = "OUTCOME1"
      val enforcementActionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
      val supervisingTeam = "Team Lincoln"
      val supervisingTeamCode = "TL01"
      val providerCode = "PC01"
      val pickUpLocationCode = "PICKUP01"
      val pickUpBuildingName = "Building 1"
      val pickUpBuildingNumber = "2"
      val pickUpStreetName = "Street 3"
      val pickUpTownCity = "Town 4"
      val pickUpCounty = "County 5"
      val pickUpPostCode = "NN11 8UU"
      val pickUpTime = LocalTime.of(12, 25)
      val date = LocalDate.of(2025, 9, 1)
      val startTime = LocalTime.of(9, 0)
      val endTime = LocalTime.of(17, 0)
      val penaltyTime = HourMinuteDuration(Duration.ofMinutes(92))
      val supervisorOfficerCode = "CRN1"
      val respondBy = LocalDate.of(2025, 10, 1)
      val hiVisWorn = true
      val workedIntensively = false
      val workQuality = NDAppointmentWorkQuality.SATISFACTORY
      val behaviour = NDAppointmentBehaviour.SATISFACTORY
      val notes = "This is a test note"

      val appointment = NDAppointment(
        id = id,
        reference = communityPaybackId,
        version = version,
        project = NDProjectAndLocation(
          name = projectName,
          code = projectCode,
          location = NDAddress.valid(),
        ),
        projectType = NDProjectType(
          name = projectTypeName,
          code = projectTypeCode,
        ),
        case = NDCaseSummary.valid().copy(
          crn = crn,
          currentExclusion = true,
        ),
        event = NDEvent.valid().copy(
          number = eventNumber,
        ),
        team = NDTeam(
          name = supervisingTeam,
          code = supervisingTeamCode,
        ),
        provider = NDProvider(
          name = "not mapped",
          code = providerCode,
        ),
        pickUpData = NDAppointmentPickUp(
          location = NDPickUpLocation(
            code = pickUpLocationCode,
            description = "Pickup location description",
            buildingName = pickUpBuildingName,
            addressNumber = pickUpBuildingNumber,
            streetName = pickUpStreetName,
            townCity = pickUpTownCity,
            county = pickUpCounty,
            postCode = pickUpPostCode,
          ),
          time = pickUpTime,
        ),
        date = date,
        startTime = startTime,
        endTime = endTime,
        penaltyHours = penaltyTime,
        supervisor = NDAppointmentSupervisor(
          code = supervisorOfficerCode,
          name = NDName.valid(),
        ),
        outcome = NDContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = NDEnforcementAction.valid().copy(
          code = "ENFORCE1",
          respondBy = respondBy,
        ),
        hiVisWorn = hiVisWorn,
        workedIntensively = workedIntensively,
        workQuality = workQuality,
        behaviour = behaviour,
        notes = notes,
        sensitive = false,
        alertActive = true,
      )

      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(code = contactOutcomeCode, attended = true)
      every { enforcementActionEntityRepository.findByCode("ENFORCE1") } returns EnforcementActionEntity.valid().copy(id = enforcementActionId)

      val result = service.toDto(appointment, ProjectTypeEntity.valid())

      assertThat(result.id).isEqualTo(id)
      assertThat(result.communityPaybackId).isEqualTo(communityPaybackId)
      assertThat(result.version).isEqualTo(version)
      assertThat(result.projectName).isEqualTo(projectName)
      assertThat(result.projectCode).isEqualTo(projectCode)
      assertThat(result.date).isEqualTo(date)
      assertThat(result.supervisingTeam).isEqualTo(supervisingTeam)
      assertThat(result.supervisingTeamCode).isEqualTo(supervisingTeamCode)
      assertThat(result.providerCode).isEqualTo(providerCode)

      val pickUpData = result.pickUpData!!
      assertThat(pickUpData.location!!.buildingName).isEqualTo(pickUpBuildingName)
      assertThat(pickUpData.location.buildingNumber).isEqualTo(pickUpBuildingNumber)
      assertThat(pickUpData.location.streetName).isEqualTo(pickUpStreetName)
      assertThat(pickUpData.location.townCity).isEqualTo(pickUpTownCity)
      assertThat(pickUpData.location.county).isEqualTo(pickUpCounty)
      assertThat(pickUpData.location.postCode).isEqualTo(pickUpPostCode)
      assertThat(pickUpData.locationCode).isEqualTo(pickUpLocationCode)
      assertThat(pickUpData.locationDescription).isEqualTo("Pickup location description")
      assertThat(pickUpData.time).isEqualTo(pickUpTime)

      assertThat(result.contactOutcomeCode).isEqualTo(contactOutcomeCode)

      assertThat(result.attendanceData?.penaltyTime).isEqualTo(HourMinuteDuration(Duration.ofMinutes(92)))
      assertThat(result.attendanceData?.penaltyMinutes).isEqualTo(92)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.SATISFACTORY)
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.SATISFACTORY)
      assertThat(result.attendanceData?.hiVisWorn).isEqualTo(hiVisWorn)
      assertThat(result.enforcementData?.enforcementActionId).isEqualTo(enforcementActionId)
      assertThat(result.enforcementData?.respondBy).isEqualTo(respondBy)

      assertThat(result.supervisorOfficerCode).isEqualTo(supervisorOfficerCode)
      assertThat(result.notes).isEqualTo(notes)

      assertThat(result.offender.crn).isEqualTo(crn)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)

      assertThat(result.deliusEventNumber).isEqualTo(98)

      assertThat(result.sensitive).isFalse
      assertThat(result.alertActive).isTrue
    }

    @Test
    fun `Populate attendance data if corresponding outcome is for attendance`() {
      val projectAppointment = NDAppointment.valid().copy(
        outcome = NDContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = NDEnforcementAction.valid().copy(code = "ENFORCE1"),
      )

      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(attended = true)
      every { enforcementActionEntityRepository.findByCode("ENFORCE1") } returns EnforcementActionEntity.valid()

      val result = service.toDto(projectAppointment, ProjectTypeEntity.valid())

      assertThat(result.attendanceData).isNotNull()
    }

    @Test
    fun `Don't populate attendance data if corresponding outcome is not for attendance`() {
      val projectAppointment = NDAppointment.valid().copy(
        outcome = NDContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = NDEnforcementAction.valid().copy(code = "ENFORCE1"),
      )

      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(attended = false)
      every { enforcementActionEntityRepository.findByCode("ENFORCE1") } returns EnforcementActionEntity.valid()

      val result = service.toDto(projectAppointment, ProjectTypeEntity.valid())

      assertThat(result.attendanceData).isNull()
    }
  }

  @Nested
  inner class ProjectAppointmentSummaryToDto {

    @Test
    fun success() {
      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(name = "The outcome")

      val result = service.toSummaryDto(
        appointmentSummary = NDAppointmentSummary(
          id = 1L,
          case = NDCaseSummary.Companion.valid().copy(crn = "CRN1"),
          outcome = NDContactOutcome.valid().copy(code = "OUTCOME1"),
          requirementProgress = NDRequirementProgress(
            requiredMinutes = 520,
            adjustments = 40,
            completedMinutes = 30,
          ),
          date = LocalDate.of(2025, 9, 1),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          daysOverdue = 0,
        ),
      )

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.contactOutcome?.name).isEqualTo("The outcome")
      assertThat(result.offender).isNotNull
      assertThat(result.requirementMinutes).isEqualTo(520)
      assertThat(result.adjustmentMinutes).isEqualTo(40)
      assertThat(result.completedMinutes).isEqualTo(30)
      assertThat(result.date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(result.startTime).isEqualTo(LocalTime.of(10, 0))
      assertThat(result.endTime).isEqualTo(LocalTime.of(11, 0))
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

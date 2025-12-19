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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Address
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentBehaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentWorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PickUpData
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Provider
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Team
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.fromDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomainEventDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toUpdateAppointment
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
  inner class AppointmentOutcomeEntityToUpdateAppointment {

    @Test
    fun success() {
      val appointmentOutcomeEntity = AppointmentOutcomeEntity(
        id = UUID.randomUUID(),
        appointmentDeliusId = 101L,
        deliusVersionToUpdate = UUID.randomUUID(),
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
        alertActive = false,
        sensitive = true,
      )

      val result = appointmentOutcomeEntity.toUpdateAppointment()

      assertThat(result.version).isEqualTo(appointmentOutcomeEntity.deliusVersionToUpdate)
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome!!.code).isEqualTo("COE1")
      assertThat(result.supervisor.code).isEqualTo("WO3736")
      assertThat(result.notes).isEqualTo("The notes")
      assertThat(result.hiVisWorn).isTrue
      assertThat(result.workedIntensively).isFalse
      assertThat(result.penaltyMinutes).isEqualTo(105)
      assertThat(result.workQuality).isEqualTo(AppointmentWorkQuality.NOT_APPLICABLE)
      assertThat(result.behaviour).isEqualTo(AppointmentBehaviour.UNSATISFACTORY)
      assertThat(result.alertActive).isFalse
      assertThat(result.sensitive).isTrue
    }

    @Test
    fun `success with only mandatory fields`() {
      val appointmentOutcomeEntity = AppointmentOutcomeEntity(
        id = UUID.randomUUID(),
        appointmentDeliusId = 101L,
        deliusVersionToUpdate = UUID.randomUUID(),
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = null,
        supervisorOfficerCode = "WO3736",
        notes = null,
        hiVisWorn = null,
        workedIntensively = null,
        penaltyMinutes = null,
        workQuality = null,
        behaviour = null,
        alertActive = null,
        sensitive = null,
      )

      val result = appointmentOutcomeEntity.toUpdateAppointment()

      assertThat(result.version).isEqualTo(appointmentOutcomeEntity.deliusVersionToUpdate)
      assertThat(result.startTime).isEqualTo(LocalTime.of(3, 2, 1))
      assertThat(result.endTime).isEqualTo(LocalTime.of(12, 11, 10))
      assertThat(result.outcome).isNull()
      assertThat(result.supervisor.code).isEqualTo("WO3736")
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

  @Nested
  inner class AppointmentOutcomeEntityToDomainEventDetail {

    @Test
    fun success() {
      val appointmentOutcomeEntity = AppointmentOutcomeEntity.valid().copy(
        id = UUID.randomUUID(),
        appointmentDeliusId = 101L,
        startTime = LocalTime.of(3, 2, 1),
        endTime = LocalTime.of(12, 11, 10),
        contactOutcome = ContactOutcomeEntity.valid().copy(code = "COE1"),
        supervisorOfficerCode = "WO3736",
        notes = "The notes",
        hiVisWorn = true,
        workedIntensively = false,
        penaltyMinutes = 105,
        workQuality = WorkQuality.NOT_APPLICABLE,
        behaviour = Behaviour.UNSATISFACTORY,
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
    }
  }

  @Nested
  inner class AppointmentToDtoMapper {
    @Test
    fun `should map ProjectAppointment to DTO correctly`() {
      val id = 101L
      val version = UUID.randomUUID()
      val projectName = "Community Garden Maintenance"
      val projectCode = "CGM101"
      val projectTypeName = "MAINTENANCE"
      val projectTypeCode = "MAINT"
      val crn = "CRN1"
      val contactOutcomeCode = "OUTCOME1"
      val enforcementActionId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
      val supervisingTeam = "Team Lincoln"
      val supervisingTeamCode = "TL01"
      val providerCode = "PC01"
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
      val penaltyTime = HourMinuteDuration(Duration.ofMinutes(0))
      val supervisorOfficerCode = "CRN1"
      val respondBy = LocalDate.of(2025, 10, 1)
      val hiVisWorn = true
      val workedIntensively = false
      val workQuality = AppointmentWorkQuality.SATISFACTORY
      val behaviour = AppointmentBehaviour.SATISFACTORY
      val notes = "This is a test note"

      val caseSummary = CaseSummary.valid().copy(
        crn = crn,
        currentExclusion = true,
      )

      val appointment = Appointment(
        id = id,
        version = version,
        project = Project(
          name = projectName,
          code = projectCode,
          location = Address.valid(),
        ),
        projectType = ProjectType(
          name = projectTypeName,
          code = projectTypeCode,
        ),
        case = caseSummary,
        team = Team(
          name = supervisingTeam,
          code = supervisingTeamCode,
        ),
        provider = Provider(
          name = "not mapped",
          code = providerCode,
        ),
        pickUpData = PickUpData(
          pickUpLocation = Address(
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
        supervisor = AppointmentSupervisor(
          code = supervisorOfficerCode,
          name = Name.valid(),
        ),
        outcome = ContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = EnforcementAction.valid().copy(
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

      val result = service.toDto(appointment)

      assertThat(result.id).isEqualTo(id)
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
      assertThat(pickUpData.time).isEqualTo(pickUpTime)

      assertThat(result.contactOutcomeCode).isEqualTo(contactOutcomeCode)

      assertThat(result.attendanceData?.penaltyTime).isEqualTo(penaltyTime)
      assertThat(result.attendanceData?.behaviour).isEqualTo(AppointmentBehaviourDto.SATISFACTORY)
      assertThat(result.attendanceData?.workQuality).isEqualTo(AppointmentWorkQualityDto.SATISFACTORY)
      assertThat(result.attendanceData?.hiVisWorn).isEqualTo(hiVisWorn)
      assertThat(result.enforcementData?.enforcementActionId).isEqualTo(enforcementActionId)
      assertThat(result.enforcementData?.respondBy).isEqualTo(respondBy)

      assertThat(result.supervisorOfficerCode).isEqualTo(supervisorOfficerCode)
      assertThat(result.notes).isEqualTo(notes)

      assertThat(result.offender.crn).isEqualTo(crn)
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)

      assertThat(result.sensitive).isFalse
      assertThat(result.alertActive).isTrue
    }

    @Test
    fun `Populate attendance data if corresponding outcome is for attendance`() {
      val projectAppointment = Appointment.valid().copy(
        outcome = ContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = EnforcementAction.valid().copy(code = "ENFORCE1"),
      )

      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(attended = true)
      every { enforcementActionEntityRepository.findByCode("ENFORCE1") } returns EnforcementActionEntity.valid()

      val result = service.toDto(projectAppointment)

      assertThat(result.attendanceData).isNotNull()
    }

    @Test
    fun `Don't populate attendance data if corresponding outcome is not for attendance`() {
      val projectAppointment = Appointment.valid().copy(
        outcome = ContactOutcome.valid().copy(code = "OUTCOME1"),
        enforcementAction = EnforcementAction.valid().copy(code = "ENFORCE1"),
      )

      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(attended = false)
      every { enforcementActionEntityRepository.findByCode("ENFORCE1") } returns EnforcementActionEntity.valid()

      val result = service.toDto(projectAppointment)

      assertThat(result.attendanceData).isNull()
    }
  }

  @Nested
  inner class ProjectAppointmentSummaryToDto {

    @Test
    fun success() {
      every { contactOutcomeEntityRepository.findByCode("OUTCOME1") } returns ContactOutcomeEntity.valid().copy(name = "The outcome")

      val result = service.toSummaryDto(
        appointmentSummary = AppointmentSummary(
          id = 1L,
          case = CaseSummary.Companion.valid().copy(crn = "CRN1"),
          outcome = ContactOutcome.valid().copy(code = "OUTCOME1"),
          requirementProgress = RequirementProgress(
            requiredMinutes = 520,
            adjustments = 40,
            completedMinutes = 30,
          ),
        ),
      )

      assertThat(result.id).isEqualTo(1L)
      assertThat(result.contactOutcome?.name).isEqualTo("The outcome")
      assertThat(result.offender).isNotNull
      assertThat(result.requirementMinutes).isEqualTo(520)
      assertThat(result.adjustmentMinutes).isEqualTo(40)
      assertThat(result.completedMinutes).isEqualTo(30)
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

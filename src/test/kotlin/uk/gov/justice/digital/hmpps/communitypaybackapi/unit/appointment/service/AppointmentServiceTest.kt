package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.EnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @MockK(relaxed = true)
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @MockK(relaxed = true)
  lateinit var domainEventService: DomainEventService

  @MockK(relaxed = true)
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK(relaxed = true)
  lateinit var offenderService: OffenderService

  @InjectMockKs
  private lateinit var service: AppointmentService

  @Nested
  inner class GetAppointment {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getAppointment(101L)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `appointment found`() {
      every {
        communityPaybackAndDeliusClient.getProjectAppointment(101L)
      } returns ProjectAppointment.valid().copy(id = 101L, crn = "CRN123")

      every { offenderService.getOffenderInfo("CRN123") } returns OffenderInfoResult.Full.valid(crn = "CRN123")

      val result = service.getAppointment(101L)

      assertThat(result.id).isEqualTo(101L)
      assertThat(result.offender.crn).isEqualTo("CRN123")
      assertThat(result.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }
  }

  @Nested
  inner class UpdateAppointmentsOutcome {

    @Test
    fun `if appointment not found, throw bad request exception`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(101L) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentsOutcome(UpdateAppointmentOutcomesDto.valid(101L))
      }.isInstanceOf(BadRequestException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entries and raise domain events`() {
      every { communityPaybackAndDeliusClient.getProjectAppointment(1L) } returns ProjectAppointment.valid().copy(crn = "CRN1")
      every { communityPaybackAndDeliusClient.getProjectAppointment(2L) } returns ProjectAppointment.valid().copy(crn = "CRN2")

      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(2L) } returns null

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.updateAppointmentsOutcome(
        UpdateAppointmentOutcomesDto(
          ids = listOf(1L, 2L),
          outcomeData = UpdateAppointmentOutcomeDto(
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
          ),
        ),
      )

      assertThat(entityCaptor).hasSize(2)

      val firstEntity = entityCaptor[0]

      assertThat(firstEntity.id).isNotNull
      assertThat(firstEntity.appointmentDeliusId).isEqualTo(1L)
      assertThat(firstEntity.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(firstEntity.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(firstEntity.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
      assertThat(firstEntity.enforcementActionId).isEqualTo(UUID.fromString("52bffba3-2366-4941-aff5-9418b4fbca7e"))
      assertThat(firstEntity.supervisorOfficerCode).isEqualTo("N45")
      assertThat(firstEntity.notes).isEqualTo("some notes")
      assertThat(firstEntity.hiVisWorn).isEqualTo(false)
      assertThat(firstEntity.workedIntensively).isEqualTo(true)
      assertThat(firstEntity.penaltyMinutes).isEqualTo(300L)
      assertThat(firstEntity.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(firstEntity.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(firstEntity.respondBy).isEqualTo(LocalDate.of(2026, 8, 10))

      verify {
        domainEventService.publish(
          id = entityCaptor[0].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 1L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN1"),
        )
      }

      verify {
        domainEventService.publish(
          id = entityCaptor[1].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 2L),
          personReferences = mapOf(PersonReferenceType.CRN to "CRN2"),
        )
      }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do not persist a new entry`() {
      val updateAppointmentDto = UpdateAppointmentOutcomesDto.valid(1L)

      val existingIdenticalEntity = service.toEntity(1L, updateAppointmentDto.outcomeData)
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L)
      } returns existingIdenticalEntity

      service.updateAppointmentsOutcome(updateAppointmentDto)

      verify(exactly = 0) { appointmentOutcomeEntityRepository.save(any()) }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry and raise domain event`() {
      val updateAppointmentDto = UpdateAppointmentOutcomesDto.valid(1L)

      val existingAlmostIdenticalEntity = service.toEntity(1L, updateAppointmentDto.outcomeData)
        .copy(notes = "some different notes")
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L)
      } returns existingAlmostIdenticalEntity

      every {
        appointmentOutcomeEntityRepository.save(any())
      } returnsArgument 0

      every { domainEventService.publish(any(), any(), any()) } just Runs

      service.updateAppointmentsOutcome(updateAppointmentDto)

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { domainEventService.publish(any(), DomainEventType.APPOINTMENT_OUTCOME, any(), any()) }
    }
  }
}

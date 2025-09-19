package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.appointment.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentAttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentEnforcementDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.Behaviour
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.WorkQuality
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @MockK
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @MockK
  lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentService

  @Nested
  inner class UpdateAppointmentsOutcome {

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entries and raise domain events`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByUpdatedAtDesc(2L) } returns null

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      every { domainEventService.publish(any(), any(), any()) } just Runs

      service.updateAppointmentsOutcome(
        UpdateAppointmentOutcomesDto(
          ids = listOf(1L, 2L),
          outcomeData = UpdateAppointmentOutcomeDto(
            projectTypeId = 3L,
            startTime = LocalTime.of(10, 1, 2),
            endTime = LocalTime.of(16, 3, 4),
            contactOutcomeId = UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"),
            supervisorTeamId = 5L,
            supervisorOfficerId = 6L,
            notes = "some notes",
            attendanceData = UpdateAppointmentAttendanceDataDto(
              hiVisWarn = false,
              workedIntensively = true,
              penaltyMinutes = 60,
              workQuality = AppointmentWorkQualityDto.SATISFACTORY,
              behaviour = AppointmentBehaviourDto.UNSATISFACTORY,
            ),
            enforcementData = UpdateAppointmentEnforcementDto(
              enforcementActionId = 12L,
              respondBy = LocalDate.of(2026, 8, 10),
            ),
          ),
        ),
      )

      assertThat(entityCaptor).hasSize(2)

      val firstEntity = entityCaptor[0]

      assertThat(firstEntity.id).isNotNull
      assertThat(firstEntity.appointmentDeliusId).isEqualTo(1L)
      assertThat(firstEntity.projectTypeDeliusId).isEqualTo(3L)
      assertThat(firstEntity.startTime).isEqualTo(LocalTime.of(10, 1, 2))
      assertThat(firstEntity.endTime).isEqualTo(LocalTime.of(16, 3, 4))
      assertThat(firstEntity.contactOutcomeId).isEqualTo(UUID.fromString("4306c7ca-b717-4995-9eea-91e41d95d44a"))
      assertThat(firstEntity.supervisorTeamDeliusId).isEqualTo(5L)
      assertThat(firstEntity.supervisorOfficerDeliusId).isEqualTo(6L)
      assertThat(firstEntity.notes).isEqualTo("some notes")
      assertThat(firstEntity.hiVisWorn).isEqualTo(false)
      assertThat(firstEntity.workedIntensively).isEqualTo(true)
      assertThat(firstEntity.penaltyMinutes).isEqualTo(60)
      assertThat(firstEntity.workQuality).isEqualTo(WorkQuality.SATISFACTORY)
      assertThat(firstEntity.behaviour).isEqualTo(Behaviour.UNSATISFACTORY)
      assertThat(firstEntity.enforcementActionDeliusId).isEqualTo(12L)
      assertThat(firstEntity.respondBy).isEqualTo(LocalDate.of(2026, 8, 10))

      verify {
        domainEventService.publish(
          id = entityCaptor[0].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 1L),
        )
      }

      verify {
        domainEventService.publish(
          id = entityCaptor[1].id,
          type = DomainEventType.APPOINTMENT_OUTCOME,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 2L),
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
      verify { domainEventService.publish(any(), DomainEventType.APPOINTMENT_OUTCOME, any()) }
    }
  }
}

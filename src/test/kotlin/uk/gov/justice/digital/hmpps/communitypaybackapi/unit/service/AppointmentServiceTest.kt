package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentOutcomeValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.util.UUID

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class AppointmentServiceTest {

  @RelaxedMockK
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentMappers: AppointmentMappers

  @RelaxedMockK
  lateinit var formService: FormService

  @RelaxedMockK
  private lateinit var appointmentOutcomeValidationService: AppointmentOutcomeValidationService

  @RelaxedMockK
  private lateinit var appointmentOutcomeEntityFactory: AppointmentOutcomeEntityFactory

  @RelaxedMockK
  private lateinit var contextService: ContextService

  @InjectMockKs
  private lateinit var service: AppointmentService

  private companion object {
    const val USERNAME = "mr-user"
  }

  @BeforeEach
  fun setupUsernameContext() {
    every { contextService.getUserName() } returns USERNAME
  }

  @Nested
  inner class GetAppointment {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every {
        communityPaybackAndDeliusClient.getAppointment(
          projectCode = "PC1",
          appointmentId = 101L,
          username = USERNAME,
        )
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getAppointment("PC1", 101L)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `appointment found`() {
      val appointment = Appointment.valid()
      every { communityPaybackAndDeliusClient.getAppointment("PC1", 101L, USERNAME) } returns appointment

      val appointmentDto = AppointmentDto.valid()
      every { appointmentMappers.toDto(appointment) } returns appointmentDto

      val result = service.getAppointment("PC1", 101L)

      assertThat(result).isSameAs(appointmentDto)
    }
  }

  @Nested
  inner class UpdateAppointmentOutcome {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(101L) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any()) } returns AppointmentOutcomeEntity.valid()
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any())
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          outcome = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 101L),
        )
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID '101'")
    }

    @Test
    fun `if appointment has newer version, throw conflict exception`() {
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null
      every { appointmentOutcomeEntityFactory.toEntity(any()) } returns AppointmentOutcomeEntity.valid()
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.updateAppointment(any(), any())
      } throws WebClientResponseExceptionFactory.conflict()

      val version = UUID.randomUUID()

      assertThatThrownBy {
        service.updateAppointmentOutcome(
          outcome = UpdateAppointmentOutcomeDto.valid().copy(
            deliusId = 1L,
            deliusVersionToUpdate = version,
          ),
        )
      }.isInstanceOf(ConflictException::class.java).hasMessage("A newer version of the appointment exists. Stale version is '$version'")
    }

    @Test
    fun `if there's no existing entries for the delius appointment ids, persist new entry and invoke update endpoint`() {
      val updateOutcomeDto = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 101L)

      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns null

      val entityReturnedByFactory = AppointmentOutcomeEntity.valid()
      every {
        appointmentOutcomeEntityFactory.toEntity(updateOutcomeDto)
      } returns entityReturnedByFactory

      val entityCaptor = mutableListOf<AppointmentOutcomeEntity>()
      every { appointmentOutcomeEntityRepository.save(capture(entityCaptor)) } returnsArgument 0

      service.updateAppointmentOutcome(
        outcome = updateOutcomeDto,
      )

      assertThat(entityCaptor).hasSize(1)
      assertThat(entityCaptor[0]).isSameAs(entityReturnedByFactory)

      verify {
        communityPaybackAndDeliusClient.updateAppointment(101L, any())
      }
    }

    @Test
    fun `if there's an existing entry and form data key is specified, remove the form data`() {
      every {
        communityPaybackAndDeliusClient.getAppointment(
          projectCode = "PC1",
          appointmentId = 101L,
          username = USERNAME,
        )
      } returns Appointment.valid().copy(case = CaseSummary.valid().copy(crn = "CRN1"))

      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        outcome = UpdateAppointmentOutcomeDto.valid().copy(
          deliusId = 101L,
          formKeyToDelete = FormKeyDto(
            id = "formKeyId",
            type = "formKeyType",
          ),
        ),
      )
    }

    @Test
    fun `if there's an existing entry for the delius appointment id and it's logically identical, do not persist a new entry`() {
      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)

      val existingIdenticalEntity = AppointmentOutcomeEntity.valid()
      every {
        appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L)
      } returns existingIdenticalEntity

      every { appointmentOutcomeEntityFactory.toEntity(updateAppointmentDto) } returns existingIdenticalEntity

      service.updateAppointmentOutcome(
        outcome = updateAppointmentDto,
      )

      verify(exactly = 0) { appointmentOutcomeEntityRepository.save(any()) }
    }

    @Test
    fun `if there's an existing entry for the delius appointment id but it's not logically identical, persist new entry send an update`() {
      val updateAppointmentDto = UpdateAppointmentOutcomeDto.valid().copy(deliusId = 1L)
      val newOutcomeEntity = AppointmentOutcomeEntity.valid()
      val existingOutcomeEntity = newOutcomeEntity.copy(contactOutcome = ContactOutcomeEntity.valid())

      every { appointmentOutcomeEntityFactory.toEntity(updateAppointmentDto) } returns newOutcomeEntity
      every { appointmentOutcomeEntityRepository.findTopByAppointmentDeliusIdOrderByCreatedAtDesc(1L) } returns existingOutcomeEntity
      every { appointmentOutcomeEntityRepository.save(any()) } returnsArgument 0

      service.updateAppointmentOutcome(
        outcome = updateAppointmentDto,
      )

      verify { appointmentOutcomeEntityRepository.save(any()) }
      verify { communityPaybackAndDeliusClient.updateAppointment(1L, any()) }
    }
  }
}

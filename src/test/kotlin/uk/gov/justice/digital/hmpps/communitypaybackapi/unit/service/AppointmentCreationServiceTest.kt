package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment

@ExtendWith(MockKExtension::class)
class AppointmentCreationServiceTest {
  @RelaxedMockK
  lateinit var appointmentValidationService: AppointmentValidationService

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentEventService: AppointmentEventService

  @RelaxedMockK
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @InjectMockKs
  private lateinit var service: AppointmentCreationService

  private companion object {
    const val CRN: String = "CRN567"
    const val DELIUS_EVENT_NUMBER: Int = 890
    const val PROJECT_CODE: String = "PROJ25"
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger.valid()
    const val ND_APPT1_ID: Long = 15
    const val ND_APPT2_ID: Long = 153
    const val PROVIDER_CODE: String = "PROV1"
    val PROJECT: ProjectDto = ProjectDto.valid().copy(providerCode = PROVIDER_CODE)
  }

  @Nested
  inner class CreateAppointments {

    @Test
    fun `ensure at least one appointment provided`() {
      assertThatThrownBy {
        service.createAppointmentsForProject(
          CreateAppointmentsDto(
            projectCode = PROJECT_CODE,
            appointments = emptyList(),
          ),
          trigger = TRIGGER,
        )
      }.hasMessage("At least one appointment must be provided")
    }

    @Test
    fun `ensure all appointments have the same project code`() {
      assertThatThrownBy {
        service.createAppointmentsForProject(
          CreateAppointmentsDto(
            projectCode = "code1",
            appointments = listOf(
              CreateAppointmentDto.valid().copy(projectCode = "code1"),
              CreateAppointmentDto.valid().copy(projectCode = "code2"),
            ),
          ),
          trigger = TRIGGER,
        )
      }.hasMessage("All appointments must be for the same project code")
    }

    @Test
    fun `create appointments persists data, sends to ND and raises a domain events`() {
      val createAppointment1Dto = CreateAppointmentDto.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER, projectCode = PROJECT_CODE)
      val createAppointment2Dto = CreateAppointmentDto.valid().copy(crn = CRN, deliusEventNumber = DELIUS_EVENT_NUMBER, projectCode = PROJECT_CODE)

      val validatedCreateAppointment1 = ValidatedAppointment.validCreateAppointment().copy(dto = createAppointment1Dto, project = PROJECT)
      every { appointmentValidationService.validateCreate(createAppointment1Dto) } returns validatedCreateAppointment1
      val validatedCreateAppointment2 = ValidatedAppointment.validCreateAppointment().copy(dto = createAppointment2Dto, project = PROJECT)
      every { appointmentValidationService.validateCreate(createAppointment2Dto) } returns validatedCreateAppointment2

      val appointmentEntity1 = createAppointment1Dto.toAppointmentEntity(ND_APPT1_ID, PROVIDER_CODE)
      val appointmentEntity2 = createAppointment2Dto.toAppointmentEntity(ND_APPT2_ID, PROVIDER_CODE)
      every { appointmentEntityRepository.saveAll(listOf(appointmentEntity1, appointmentEntity2)) } returnsArgument 0

      every {
        communityPaybackAndDeliusClient.createAppointments(
          projectCode = PROJECT_CODE,
          NDCreateAppointments(listOf(validatedCreateAppointment1.toNDCreateAppointment(), validatedCreateAppointment2.toNDCreateAppointment())),
        )
      } returns listOf(
        NDCreatedAppointment(id = ND_APPT1_ID, reference = createAppointment1Dto.id),
        NDCreatedAppointment(id = ND_APPT2_ID, reference = createAppointment2Dto.id),
      )

      val creationEvent1 = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        appointment = appointmentEntity1,
      )
      every {
        appointmentEventService.buildCreatedEvent(
          appointment = appointmentEntity1,
          trigger = TRIGGER,
          validatedCreateAppointmentDto = validatedCreateAppointment1,
        )
      } returns creationEvent1

      val creationEvent2 = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        appointment = appointmentEntity2,
      )

      every {
        appointmentEventService.buildCreatedEvent(
          appointment = appointmentEntity2,
          trigger = TRIGGER,
          validatedCreateAppointmentDto = validatedCreateAppointment2,
        )
      } returns creationEvent2

      val result = service.createAppointmentsForProject(
        CreateAppointmentsDto(
          projectCode = PROJECT_CODE,
          appointments = listOf(createAppointment1Dto, createAppointment2Dto),
        ),
        trigger = TRIGGER,
      )

      assertThat(result).containsExactlyInAnyOrder(ND_APPT1_ID, ND_APPT2_ID)

      verify {
        appointmentEntityRepository.saveAll(
          listOf(
            createAppointment1Dto.toAppointmentEntity(ND_APPT1_ID, PROVIDER_CODE),
            createAppointment2Dto.toAppointmentEntity(ND_APPT2_ID, PROVIDER_CODE),
          ),
        )

        appointmentEventService.saveAndThenPublishOnTransactionCommit(listOf(creationEvent1, creationEvent2))
      }
    }
  }
}

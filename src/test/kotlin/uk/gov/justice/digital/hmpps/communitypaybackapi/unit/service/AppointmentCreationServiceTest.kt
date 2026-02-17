package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdditionalInformationType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DomainEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonReferenceType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment

@ExtendWith(MockKExtension::class)
class AppointmentCreationServiceTest {
  @RelaxedMockK
  lateinit var appointmentValidationService: AppointmentValidationService

  @RelaxedMockK
  lateinit var appointmentEventEntityFactory: AppointmentEventEntityFactory

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @RelaxedMockK
  lateinit var domainEventService: DomainEventService

  @InjectMockKs
  private lateinit var service: AppointmentCreationService

  private companion object {
    const val PROJECT_CODE: String = "PROJ25"
    val TRIGGER: AppointmentEventTrigger = AppointmentEventTrigger.valid()
  }

  @Nested
  inner class CreateAppointment {

    @Test
    fun `create appointments sends to ND, persists events, raises a domain events`() {
      val createAppointment1Dto = CreateAppointmentDto.valid()
      val createAppointment2Dto = CreateAppointmentDto.valid()
      val createAppointmentsDto = CreateAppointmentsDto(
        projectCode = PROJECT_CODE,
        appointments = listOf(createAppointment1Dto, createAppointment2Dto),
      )

      val creationEvent1 = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        communityPaybackAppointmentId = createAppointment1Dto.id,
      )
      every {
        appointmentEventEntityFactory.buildCreatedEvent(
          deliusId = 0,
          trigger = TRIGGER,
          createAppointmentDto = createAppointment1Dto,
          projectCode = PROJECT_CODE,
        )
      } returns creationEvent1

      val creationEvent2 = AppointmentEventEntity.valid().copy(
        eventType = AppointmentEventType.CREATE,
        communityPaybackAppointmentId = createAppointment2Dto.id,
      )
      every {
        appointmentEventEntityFactory.buildCreatedEvent(
          deliusId = 0,
          trigger = TRIGGER,
          createAppointmentDto = createAppointment2Dto,
          projectCode = PROJECT_CODE,
        )
      } returns creationEvent2

      every {
        communityPaybackAndDeliusClient.createAppointments(any(), any())
      } returns listOf(
        NDCreatedAppointment(id = 15, reference = createAppointment1Dto.id),
        NDCreatedAppointment(id = 153, reference = createAppointment2Dto.id),
      )

      service.createAppointments(
        createAppointments = createAppointmentsDto,
        trigger = TRIGGER,
      )

      verify {
        communityPaybackAndDeliusClient.createAppointments(
          projectCode = PROJECT_CODE,
          appointments = NDCreateAppointments(listOf(creationEvent1.toNDCreateAppointment(), creationEvent2.toNDCreateAppointment())),
        )

        appointmentEventEntityRepository.saveAll(
          listOf(
            creationEvent1.copy(deliusAppointmentId = 15L),
            creationEvent2.copy(deliusAppointmentId = 153L),
          ),
        )

        domainEventService.publishOnTransactionCommit(
          id = creationEvent1.id,
          type = DomainEventType.APPOINTMENT_CREATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 15L),
          personReferences = mapOf(PersonReferenceType.CRN to creationEvent1.crn),
        )
        domainEventService.publishOnTransactionCommit(
          id = creationEvent2.id,
          type = DomainEventType.APPOINTMENT_CREATED,
          additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to 153L),
          personReferences = mapOf(PersonReferenceType.CRN to creationEvent2.crn),
        )
      }
    }
  }
}

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentEventEntityFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentCreationServiceTest {
  @RelaxedMockK
  lateinit var appointmentEventEntityFactory: AppointmentEventEntityFactory

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentEventEntityRepository: AppointmentEventEntityRepository

  @InjectMockKs
  private lateinit var service: AppointmentCreationService

  private companion object {
    const val PROJECT_CODE: String = "PROJ25"
    val SCHEDULING_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class CreateAppointment {

    @Test
    fun `create appointments`() {
      val createAppointment1Dto = CreateAppointmentDto.valid()
      val createAppointment2Dto = CreateAppointmentDto.valid()
      val createAppointmentsDto = CreateAppointmentsDto(
        projectCode = PROJECT_CODE,
        appointments = listOf(createAppointment1Dto, createAppointment2Dto),
      )

      val creationEvent1 = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.CREATE)
      every {
        appointmentEventEntityFactory.buildCreatedEvent(
          projectCode = PROJECT_CODE,
          deliusId = 0,
          triggeredBySchedulingId = SCHEDULING_ID,
          createAppointmentDto = createAppointment1Dto,
        )
      } returns creationEvent1

      val creationEvent2 = AppointmentEventEntity.valid().copy(eventType = AppointmentEventType.CREATE)
      every {
        appointmentEventEntityFactory.buildCreatedEvent(
          projectCode = PROJECT_CODE,
          deliusId = 0,
          triggeredBySchedulingId = SCHEDULING_ID,
          createAppointmentDto = createAppointment2Dto,
        )
      } returns creationEvent2

      every {
        communityPaybackAndDeliusClient.createAppointments(any(), any())
      } returns listOf(
        NDCreatedAppointment(id = 15),
        NDCreatedAppointment(id = 153),
      )

      service.createAppointments(
        createAppointments = createAppointmentsDto,
        triggeredBySchedulingId = SCHEDULING_ID,
      )

      verify {
        communityPaybackAndDeliusClient.createAppointments(
          projectCode = PROJECT_CODE,
          appointments = NDCreateAppointments(listOf(creationEvent1.toNDCreateAppointment(), creationEvent2.toNDCreateAppointment())),
        )
      }

      verify {
        appointmentEventEntityRepository.saveAll(
          listOf(
            creationEvent1.copy(appointmentDeliusId = 15L),
            creationEvent2.copy(appointmentDeliusId = 153L),
          ),
        )
      }
    }
  }
}

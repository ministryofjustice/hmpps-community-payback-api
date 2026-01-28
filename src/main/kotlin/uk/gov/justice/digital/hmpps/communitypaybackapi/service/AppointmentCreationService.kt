package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment

@Service
class AppointmentCreationService(
  val appointmentEventEntityFactory: AppointmentEventEntityFactory,
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val appointmentEventEntityRepository: AppointmentEventEntityRepository,
) {

  @Transactional
  fun createAppointments(
    createAppointments: CreateAppointmentsDto,
    trigger: AppointmentEventTrigger,
  ) {
    val projectCode = createAppointments.projectCode

    val appointmentCreationEvents = createAppointments.appointments.map { createAppointment ->
      appointmentEventEntityFactory.buildCreatedEvent(
        projectCode = projectCode,
        // the ID will be provided by the upstream response and set on the event before persistence
        deliusId = 0L,
        trigger = trigger,
        createAppointmentDto = createAppointment,
      )
    }

    val appointmentCreateRequests = NDCreateAppointments(
      appointments = appointmentCreationEvents.map { it.toNDCreateAppointment() },
    )

    val creationResponse = communityPaybackAndDeliusClient.createAppointments(
      projectCode = projectCode,
      appointments = appointmentCreateRequests,
    )

    val requestCount = appointmentCreateRequests.appointments.size
    require(creationResponse.size == appointmentCreateRequests.appointments.size) {
      "Expected $requestCount appointments to be created, but was ${creationResponse.size}"
    }

    val appointmentCreationEventsWithIds = appointmentCreationEvents.mapIndexed { index, event ->
      event.copy(deliusAppointmentId = creationResponse[index].id)
    }

    appointmentEventEntityRepository.saveAll(appointmentCreationEventsWithIds)
  }
}

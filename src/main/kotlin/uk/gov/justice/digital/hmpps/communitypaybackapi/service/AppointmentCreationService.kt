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
  private val appointmentEventEntityFactory: AppointmentEventEntityFactory,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentEventEntityRepository: AppointmentEventEntityRepository,
  private val domainEventService: DomainEventService,
  private val projectService: ProjectService,
) {

  @Transactional
  fun createAppointments(
    createAppointments: CreateAppointmentsDto,
    trigger: AppointmentEventTrigger,
  ) {
    val projectCode = createAppointments.projectCode

    val appointmentCreationEvents = createAppointments.appointments.map { createAppointment ->
      appointmentEventEntityFactory.buildCreatedEvent(
        // the ID will be provided by the upstream response and set on the event before persistence
        deliusId = 0L,
        trigger = trigger,
        createAppointmentDto = createAppointment,
        project = projectService.getProject(projectCode),
      )
    }

    val appointmentCreateRequests = NDCreateAppointments(
      appointments = appointmentCreationEvents.map { it.toNDCreateAppointment() },
    )

    val creationResponse = communityPaybackAndDeliusClient.createAppointments(
      projectCode = projectCode,
      appointments = appointmentCreateRequests,
    )

    val appointmentCreationEventsWithIds = appointmentCreationEvents.map { event ->
      event.copy(deliusAppointmentId = creationResponse.first { it.reference == event.communityPaybackAppointmentId!! }.id)
    }

    appointmentEventEntityRepository.saveAll(appointmentCreationEventsWithIds)

    appointmentCreationEventsWithIds.forEach { event ->
      domainEventService.publishOnTransactionCommit(
        id = event.id,
        type = DomainEventType.APPOINTMENT_CREATED,
        additionalInformation = mapOf(AdditionalInformationType.APPOINTMENT_ID to event.deliusAppointmentId),
        personReferences = mapOf(PersonReferenceType.CRN to event.crn),
      )
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment

@Service
class AppointmentCreationService(
  private val appointmentEventEntityFactory: AppointmentEventEntityFactory,
  private val appointmentValidationService: AppointmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentEventEntityRepository: AppointmentEventEntityRepository,
  private val domainEventService: DomainEventService,
) {

  @Transactional
  fun createAppointment(
    appointment: CreateAppointmentDto,
    trigger: AppointmentEventTrigger,
  ) = createAppointmentsForProject(
    listOf(appointment),
    trigger,
  )

  @Transactional
  fun createAppointmentsForProject(
    appointments: List<CreateAppointmentDto>,
    trigger: AppointmentEventTrigger,
  ) {
    require(appointments.isNotEmpty()) { "At least one appointment must be provided" }
    require(appointments.map { it.projectCode }.toSet().size == 1) { "All appointments must be for the same project code" }

    val projectCode = appointments[0].projectCode

    val appointmentCreationEvents = appointments.map { createAppointment ->
      appointmentEventEntityFactory.buildCreatedEvent(
        // the ID will be provided by the upstream response and set on the event before persistence
        deliusId = 0L,
        trigger = trigger,
        validatedCreateAppointmentDto = appointmentValidationService.validateCreate(createAppointment),
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

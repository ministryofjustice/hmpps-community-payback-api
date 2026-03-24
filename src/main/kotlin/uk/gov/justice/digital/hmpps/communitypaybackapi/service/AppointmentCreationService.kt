package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment
import java.util.UUID

@Service
class AppointmentCreationService(
  private val appointmentEventService: AppointmentEventService,
  private val appointmentValidationService: AppointmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentEntityRepository: AppointmentEntityRepository,
) {

  @Transactional
  fun createAppointment(
    appointment: CreateAppointmentDto,
    trigger: AppointmentEventTrigger,
  ): Long = createAppointmentsForProject(
    listOf(appointment),
    trigger,
  ).first()

  @Transactional
  fun createAppointmentsForProject(
    appointments: List<CreateAppointmentDto>,
    trigger: AppointmentEventTrigger,
  ): List<Long> {
    require(appointments.isNotEmpty()) { "At least one appointment must be provided" }
    require(appointments.map { it.projectCode }.toSet().size == 1) { "All appointments must be for the same project code" }

    val projectCode = appointments[0].projectCode

    val validatedAppointments = appointments.map { appointmentValidationService.validateCreate(it) }

    val creationResponse = communityPaybackAndDeliusClient.createAppointments(
      projectCode = projectCode,
      appointments = NDCreateAppointments(validatedAppointments.map { it.toNDCreateAppointment() }),
    )

    appointmentEntityRepository.saveAll(
      appointments.map {
        it.toAppointmentEntity(
          creationResponse.findDeliusId(communityPaybackId = it.id),
        )
      },
    )

    appointmentEventService.saveAndThenPublishOnTransactionCommit(
      appointments.map { createAppointment ->
        appointmentEventService.buildCreatedEvent(
          deliusId = creationResponse.findDeliusId(communityPaybackId = createAppointment.id),
          trigger = trigger,
          validatedCreateAppointmentDto = appointmentValidationService.validateCreate(createAppointment),
        )
      },
    )

    return creationResponse.map { it.id }
  }

  private fun List<NDCreatedAppointment>.findDeliusId(communityPaybackId: UUID) = first { it.reference == communityPaybackId }.id
}

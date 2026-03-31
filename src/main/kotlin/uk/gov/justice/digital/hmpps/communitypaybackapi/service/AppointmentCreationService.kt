package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
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
    CreateAppointmentsDto(
      projectCode = appointment.projectCode,
      appointments = listOf(appointment),
    ),
    trigger,
  ).single()

  @Transactional
  fun createAppointmentsForProject(
    createAppointmentsDto: CreateAppointmentsDto,
    trigger: AppointmentEventTrigger,
  ): List<Long> {
    val projectCode = createAppointmentsDto.projectCode
    val appointments = createAppointmentsDto.appointments

    require(createAppointmentsDto.appointments.isNotEmpty()) { "At least one appointment must be provided" }
    require(createAppointmentsDto.appointments.count { it.projectCode != createAppointmentsDto.projectCode } == 0) { "All appointments must be for the same project code" }

    val validatedAppointments = appointments.map { appointmentValidationService.validateCreate(it) }

    val creationResponse = communityPaybackAndDeliusClient.createAppointments(
      projectCode = projectCode,
      appointments = NDCreateAppointments(validatedAppointments.map { it.toNDCreateAppointment() }),
    )

    val appointmentEntities = appointmentEntityRepository.saveAll(
      validatedAppointments.map {
        it.dto.toAppointmentEntity(
          deliusAppointmentId = creationResponse.findDeliusId(communityPaybackId = it.dto.id),
          providerCode = it.project.providerCode,
        )
      },
    )

    appointmentEventService.persistAndPublishAppointmentCreatedDomainEvents(
      validatedAppointments.map { validatedCreateAppointment ->
        CommunityPaybackSpringEvent.AppointmentCreatedEvent(
          createDto = validatedCreateAppointment,
          appointmentEntity = appointmentEntities.first { it.id == validatedCreateAppointment.dto.id },
          trigger = trigger,
        )
      },
    )

    return creationResponse.map { it.id }
  }

  private fun List<NDCreatedAppointment>.findDeliusId(communityPaybackId: UUID) = first { it.reference == communityPaybackId }.id
}

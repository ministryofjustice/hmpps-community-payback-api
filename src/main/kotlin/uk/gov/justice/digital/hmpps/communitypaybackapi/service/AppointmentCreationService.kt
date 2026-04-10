package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDCreateAppointment
import java.util.UUID

@Service
class AppointmentCreationService(
  private val appointmentValidationService: AppointmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentEntityRepository: AppointmentEntityRepository,
  private val springEventPublisher: SpringEventPublisher,
  private val appointmentIdGenerator: AppointmentIdGenerator,
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

    val appointmentsToCreate = appointments.map {
      AppointmentToCreate(
        id = appointmentIdGenerator.generateId(),
        validatedAppointment = appointmentValidationService.validateCreate(it),
      )
    }

    val creationResponse = communityPaybackAndDeliusClient.createAppointments(
      projectCode = projectCode,
      appointments = NDCreateAppointments(appointmentsToCreate.map { it.validatedAppointment.toNDCreateAppointment(it.id) }),
    )

    val appointmentEntities = appointmentEntityRepository.saveAll(
      appointmentsToCreate.map {
        it.validatedAppointment.dto.toAppointmentEntity(
          id = it.id,
          deliusAppointmentId = creationResponse.findDeliusId(communityPaybackId = it.id),
          providerCode = it.validatedAppointment.project.providerCode,
        )
      },
    )

    appointmentsToCreate.forEach { appointmentToCreate ->
      springEventPublisher.publishEvent(
        CommunityPaybackSpringEvent.AppointmentCreatedEvent(
          createDto = appointmentToCreate.validatedAppointment,
          appointmentEntity = appointmentEntities.first { it.id == appointmentToCreate.id },
          trigger = trigger,
        ),
      )
    }

    return creationResponse.map { it.id }
  }

  data class AppointmentToCreate(
    val id: UUID,
    val validatedAppointment: ValidatedAppointment<CreateAppointmentDto>,
  )

  private fun List<NDCreatedAppointment>.findDeliusId(communityPaybackId: UUID) = first { it.reference == communityPaybackId }.id
}

interface AppointmentIdGenerator {
  fun generateId(): UUID
}

@Service
class DefaultAppointmentIdGenerator : AppointmentIdGenerator {
  override fun generateId() = AppointmentEntity.generateId()
}

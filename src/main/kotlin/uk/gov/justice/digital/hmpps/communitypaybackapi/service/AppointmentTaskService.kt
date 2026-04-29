package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentUpdatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentTaskMappers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppointmentTaskService(
  private val appointmentTaskEntityRepository: AppointmentTaskEntityRepository,
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val contextService: ContextService,
  private val caseVisibilityService: CaseVisibilityService,
  private val appointmentTaskMappers: AppointmentTaskMappers,
) {

  @EventListener
  @Transactional(Transactional.TxType.REQUIRED)
  fun createTravelTimeTaskOnAppointmentCreation(
    event: AppointmentCreatedEvent,
  ) {
    createTravelTimeTaskIfRequired(
      appointment = event.appointmentEntity,
      outcome = event.createDto.contactOutcome,
      project = event.createDto.project,
    )
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRED)
  fun createTravelTimeTaskOnAppointmentUpdate(
    event: AppointmentUpdatedEvent,
  ) {
    createTravelTimeTaskIfRequired(
      appointment = event.appointmentEntity,
      outcome = event.updateDto.contactOutcome,
      project = event.updateDto.project,
    )
  }

  @EventListener
  @Transactional(Transactional.TxType.REQUIRED)
  fun closeTravelTimeTaskOnAdjustmentCreation(
    event: AdjustmentCreatedEvent,
  ) {
    val trigger = event.trigger
    if (trigger.triggerType == AdjustmentEventTriggerType.APPOINTMENT_TASK) {
      val taskId = UUID.fromString(trigger.triggeredBy)
      val task = appointmentTaskEntityRepository.findByIdOrNull(taskId) ?: error("Can't find task with id $taskId for adjustment ${event.deliusAdjustmentId}")
      task.taskStatus = AppointmentTaskStatus.COMPLETE
      task.decisionMadeAt = trigger.triggeredAt
      task.decisionMadeByUsername = contextService.getUserName()
      task.decisionDescription = "Task completed on adjustment creation"
      appointmentTaskEntityRepository.save(task)
    }
  }

  fun taskExists(taskId: UUID) = appointmentTaskEntityRepository.existsById(taskId)

  @Transactional
  fun completeTask(
    taskId: UUID,
  ) {
    val task = appointmentTaskEntityRepository.findByIdOrNull(taskId) ?: badRequest("Task not found for ID '$taskId'")
    task.taskStatus = AppointmentTaskStatus.COMPLETE
    task.decisionMadeAt = OffsetDateTime.now()
    task.decisionMadeByUsername = contextService.getUserName()
    task.decisionDescription = "Task completed directly"
    appointmentTaskEntityRepository.save(task)
  }

  private fun createTravelTimeTaskIfRequired(
    appointment: AppointmentEntity,
    outcome: ContactOutcomeEntity?,
    project: ProjectDto,
  ) {
    val attended = outcome?.attended == true
    val projectSupportsTravelTime = project.projectType.group?.let { ProjectTypeGroup.fromDto(it).travelTimeSupported } == true
    if (attended && projectSupportsTravelTime) {
      appointmentTaskEntityRepository.save(
        AppointmentTaskEntity(
          id = UUID.randomUUID(),
          appointment = appointment,
          taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
          taskStatus = AppointmentTaskStatus.PENDING,
        ),
      )
    }
  }

  fun getPendingAppointmentTasks(
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    providerCode: String? = null,
    pageable: Pageable,
  ): Page<AppointmentTaskSummaryDto> {
    val pagedTasks = appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
      fromDate = fromDate,
      toDate = toDate,
      providerCode = providerCode,
      pageable = pageable,
    )

    val orderedTasks = pagedTasks.toList()
    if (orderedTasks.isEmpty()) {
      return PageImpl(emptyList(), pageable, 0)
    }

    val deliusAppointmentIds = orderedTasks.map { it.appointment.deliusId }
    val appointments = appointmentRetrievalService.getAppointments(
      deliusAppointmentIds = deliusAppointmentIds.sorted(),
      pageable = PageRequest.of(0, deliusAppointmentIds.size, Sort.by(Sort.Direction.DESC, "name")),
    )

    val caseVisibility = caseVisibilityService.isLimitedForCurrentUser(orderedTasks.map { it.appointment.crn })

    return PageImpl(
      pagedTasks.content.map { task ->
        appointmentTaskMappers.toDto(
          task = task,
          isLimited = caseVisibility[task.appointment.crn] ?: false,
          appointment = appointments.content.first { it.id == task.appointment.deliusId },
        )
      },
      pageable,
      pagedTasks.totalElements,
    )
  }
}

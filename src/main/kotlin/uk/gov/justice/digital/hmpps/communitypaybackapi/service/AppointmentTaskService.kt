package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import java.time.LocalDate

@Service
class AppointmentTaskService(
  private val appointmentTaskEntityRepository: AppointmentTaskEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val contextService: ContextService,
  private val projectService: ProjectService,
) {
  fun getPendingAppointmentTasks(
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    providerCode: String? = null,
    pageable: Pageable,
  ): Page<AppointmentTaskSummaryDto> {
    val tasksWithAppointments = appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
      fromDate = fromDate,
      toDate = toDate,
      providerCode = providerCode,
      pageable = pageable,
    )

    val taskSummaries = tasksWithAppointments.content.map { (task, appointmentEntity) ->
      val fullAppointment = communityPaybackAndDeliusClient.getAppointment(
        projectCode = appointmentEntity.providerCode,
        appointmentId = appointmentEntity.deliusId,
        username = contextService.getUserName(),
      )
      val projectType = projectService.getProjectTypeForCode(fullAppointment.projectType.code)
        ?: error("Can't resolve project type for code ${fullAppointment.projectType.code}")
      val appointmentDto = appointmentMappers.toDto(fullAppointment, projectType)
      val appointmentSummary = appointmentMappers.toSummaryDtoFromDto(appointmentDto)

      AppointmentTaskSummaryDto(taskId = task.id, appointment = appointmentSummary)
    }

    return PageImpl(taskSummaries, pageable, tasksWithAppointments.totalElements)
  }
}

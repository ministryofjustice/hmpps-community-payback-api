package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import java.time.LocalDate

@Service
class AppointmentTaskService(
  private val appointmentTaskEntityRepository: AppointmentTaskEntityRepository,
  private val appointmentRetrievalService: AppointmentRetrievalService,
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

    if (tasksWithAppointments.isEmpty) {
      return PageImpl(emptyList(), pageable, 0)
    }

    val taskMap = tasksWithAppointments.content.associateBy { it.appointment.deliusId }
    val appointmentIds = taskMap.keys.toList()

    val appointmentsPage = appointmentRetrievalService.getAppointments(
      appointmentIds = appointmentIds,
      pageable = PageRequest.of(0, appointmentIds.size, Sort.by(Sort.Direction.DESC, "name")),
    )

    val taskSummaries = appointmentsPage.content.mapNotNull { appointment ->
      taskMap[appointment.id]?.let { task ->
        AppointmentTaskSummaryDto(taskId = task.id, appointment = appointment)
      }
    }

    return PageImpl(taskSummaries, pageable, tasksWithAppointments.totalElements)
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import java.time.LocalDate

@Service
class AppointmentTaskService(
  private val appointmentTaskEntityRepository: AppointmentTaskEntityRepository,
) {
  fun getPendingAppointmentTasks(
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    providerCode: String? = null,
    pageable: Pageable,
  ): Page<AppointmentTaskSummaryDto> = appointmentTaskEntityRepository.findPendingTasksWithFilters(
    fromDate = fromDate,
    toDate = toDate,
    providerCode = providerCode,
    pageable = pageable,
  ).map { task ->
    AppointmentTaskSummaryDto(taskId = task.id)
  }
}

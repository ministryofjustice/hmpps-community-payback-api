package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity

@Service
class AppointmentTaskMappers {
  fun toDto(
    task: AppointmentTaskEntity,
    isLimited: Boolean,
    appointment: AppointmentSummaryDto,
  ) = AppointmentTaskSummaryDto(
    taskId = task.id,
    offender = if (isLimited) {
      OffenderDto.OffenderLimitedDto(task.appointment.crn)
    } else {
      OffenderDto.OffenderFullDto(
        task.appointment.crn,
        task.appointment.firstName,
        task.appointment.lastName,
        null,
        null,
      )
    },
    date = task.appointment.date,
    projectTypeName = task.appointment.projectType?.name,
    appointment = appointment,
  )
}

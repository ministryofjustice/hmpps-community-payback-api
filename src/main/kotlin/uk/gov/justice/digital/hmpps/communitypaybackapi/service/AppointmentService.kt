package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import java.time.LocalDate

@Service
class AppointmentService(
  private val appointmentBulkUpdateService: AppointmentBulkUpdateService,
  private val appointmentCreationService: AppointmentCreationService,
  private val appointmentRetrievalService: AppointmentRetrievalService,
  private val appointmentUpdateService: AppointmentUpdateService,
) {

  fun createAppointment(
    appointment: CreateAppointmentDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentCreationService.createAppointment(appointment, trigger)

  fun createAppointmentsForProject(
    appointments: CreateAppointmentsDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentCreationService.createAppointmentsForProject(appointments, trigger)

  fun getAppointment(
    projectCode: String,
    appointmentId: Long,
  ) = appointmentRetrievalService.getAppointment(projectCode, appointmentId)

  fun getAppointments(
    crn: String? = null,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    outcomeCodes: List<String>? = null,
    projectCodes: List<String>? = null,
    projectTypeGroup: ProjectTypeGroupDto? = null,
    pageable: Pageable,
  ) = appointmentRetrievalService.getAppointments(crn, fromDate, toDate, outcomeCodes, projectCodes, projectTypeGroup, pageable)

  fun updateAppointmentOutcome(
    projectCode: String,
    update: UpdateAppointmentOutcomeDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentUpdateService.updateAppointmentOutcome(projectCode, update, trigger)

  fun updateAppointmentOutcomes(
    projectCode: String,
    request: UpdateAppointmentOutcomesDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentBulkUpdateService.updateAppointmentOutcomes(projectCode, request, trigger)
}

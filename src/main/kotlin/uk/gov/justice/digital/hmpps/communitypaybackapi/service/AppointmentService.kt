package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
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
    deliusAppointmentId: Long,
  ) = appointmentRetrievalService.getAppointment(projectCode, deliusAppointmentId)

  fun getAppointments(
    crn: String? = null,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    outcomeCodes: List<String>? = null,
    projectCodes: List<String>? = null,
    projectTypeGroup: ProjectTypeGroupDto? = null,
    eventNumber: String? = null,
    deliusAppointmentIds: List<Long>? = null,
    pageable: Pageable,
  ) = appointmentRetrievalService.getAppointments(
    crn = crn,
    fromDate = fromDate,
    toDate = toDate,
    outcomeCodes = outcomeCodes,
    projectCodes = projectCodes,
    projectTypeGroup = projectTypeGroup,
    eventNumber = eventNumber,
    deliusAppointmentIds = deliusAppointmentIds,
    pageable = pageable,
  )

  fun updateAppointment(
    existingAppointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentUpdateService.updateAppointment(existingAppointment, update, trigger)

  fun updateAppointments(
    projectCode: String,
    request: UpdateAppointmentOutcomesDto,
    trigger: AppointmentEventTrigger,
  ) = appointmentBulkUpdateService.updateAppointments(projectCode, request, trigger)
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toHttpParams
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import java.time.LocalDate

@Service
class AppointmentRetrievalService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val contextService: ContextService,
  private val projectService: ProjectService,
) {

  fun getAppointment(
    projectCode: String,
    appointmentId: Long,
  ): AppointmentDto = try {
    communityPaybackAndDeliusClient.getAppointment(
      projectCode = projectCode,
      appointmentId = appointmentId,
      username = contextService.getUserName(),
    ).let { appointment ->
      val projectTypeCode = appointment.projectType.code
      val projectType = projectService.getProjectTypeForCode(projectTypeCode) ?: error("Can't resolve project type for code $projectTypeCode")

      appointmentMappers.toDto(appointment, projectType)
    }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", "Project $projectCode, ID $appointmentId")
  }

  fun getAppointments(
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    outcomeCodes: List<String>?,
    projectCodes: List<String>?,
    projectTypeGroup: ProjectTypeGroupDto?,
    pageable: Pageable,
  ): Page<AppointmentSummaryDto> {
    val pageResponse = communityPaybackAndDeliusClient.getAppointments(
      crn = crn,
      fromDate = fromDate,
      toDate = toDate,
      outcomeCodes = outcomeCodes,
      projectCodes = projectCodes,
      projectTypeCodes = projectTypeGroup?.let { projectTypeGroup -> projectService.projectTypesForGroup(projectTypeGroup).map { it.code } },
      params = pageable.toHttpParams(),
    )
    return PageImpl(pageResponse.content.map { appointmentMappers.toSummaryDto(it) }, pageable, pageResponse.page.totalElements)
  }
}

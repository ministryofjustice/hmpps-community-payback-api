package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.asPage
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toHttpParams
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class SessionService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val sessionMappers: SessionMappers,
  private val sessionSupervisorEntityRepository: SessionSupervisorEntityRepository,
  private val contextService: ContextService,
  private val projectService: ProjectService,
  private val appointmentService: AppointmentService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getSessions(
    teamCodes: List<String>,
    startDate: LocalDate,
    endDate: LocalDate,
    projectTypeGroup: ProjectTypeGroupDto?,
    pageable: Pageable,
  ): Page<SessionSummaryDto> {
    val projectTypeCodes = projectTypeGroup?.let { projectTypeGroup ->
      projectService.projectTypesForGroup(projectTypeGroup).map { it.code }
    } ?: emptyList()

    return getSessions(teamCodes, startDate, endDate, projectTypeCodes, pageable)
  }

  fun getSessions(
    teamCodes: List<String>,
    startDate: LocalDate,
    endDate: LocalDate,
    projectTypeCodes: List<String>,
    pageable: Pageable,
  ): Page<SessionSummaryDto> {
    if (ChronoUnit.DAYS.between(startDate, endDate) > 7) {
      badRequest("Date range cannot be greater than 7 days")
    }

    val response = communityPaybackAndDeliusClient.getSessions(
      teamCodes = teamCodes,
      startDate = startDate,
      endDate = endDate,
      typeCode = projectTypeCodes,
      params = pageable.toHttpParams(),
    )

    return response.asPage(pageable) { it.toDto() }
  }

  fun getSession(
    projectCode: String,
    date: LocalDate,
  ): SessionDto {
    val project = projectService.getProject(projectCode) ?: error("Can't retrieve project $projectCode")
    val appointments = getSessionAppointments(projectCode, date)

    return sessionMappers.toSessionDto(
      date = date,
      project = project,
      appointments = appointments,
    )
  }

  fun assignSupervisor(
    projectCode: String,
    day: LocalDate?,
    supervisorCode: String,
    assignedByUsername: String = contextService.getUserName(),
  ) {
    val existing = sessionSupervisorEntityRepository.findByProjectCodeAndDayAndSupervisorCode(projectCode, day, supervisorCode)
    sessionSupervisorEntityRepository.save(
      existing?.apply { this.assignedByUsername = assignedByUsername }
        ?: SessionSupervisorEntity(
          projectCode = projectCode,
          day = day,
          supervisorCode = supervisorCode,
          assignedByUsername = assignedByUsername,
        ),
    )
    log.info("Project [{}], date [{}] assigned to [{}]", projectCode, day, supervisorCode)
  }

  fun unassignSupervisor(projectCode: String, day: LocalDate?, supervisorCode: String) {
    sessionSupervisorEntityRepository.findByProjectCodeAndDayAndSupervisorCode(projectCode, day, supervisorCode)?.let {
      sessionSupervisorEntityRepository.delete(it)
    }
  }

  fun getNextAssignmentForSupervisor(supervisorCode: String): SessionSummaryDto? {
    val today = LocalDate.now()
    val candidates = sessionSupervisorEntityRepository.findAssignments(supervisorCode, today).mapNotNull { assignment ->
      val date = assignment.day ?: appointmentService.getAppointments(
        fromDate = today,
        projectCodes = listOf(assignment.projectCode),
        pageable = PageRequest.of(0, 1, Sort.by("date")),
      ).content.firstOrNull()?.date
      date?.let { assignment.projectCode to it }
    }
    val (projectCode, date) = candidates.minByOrNull { it.second } ?: return null
    return sessionMappers.toSummaryDto(
      date = date,
      project = projectService.getProject(projectCode) ?: error("Can't retrieve project $projectCode"),
      appointments = getSessionAppointments(projectCode, date),
    )
  }

  private fun getSessionAppointments(
    projectCode: String,
    date: LocalDate,
  ) = appointmentService.getAppointments(
    fromDate = date,
    toDate = date,
    projectCodes = listOf(projectCode),
    pageable = PageRequest.of(0, Int.MAX_VALUE, Sort.by("name")),
  ).content
}

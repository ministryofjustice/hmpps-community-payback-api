package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorId
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
    providerCode: String,
    teamCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    projectTypeGroup: ProjectTypeGroupDto?,
  ): SessionSummariesDto {
    if (ChronoUnit.DAYS.between(startDate, endDate) > 7) {
      badRequest("Date range cannot be greater than 7 days")
    }

    return communityPaybackAndDeliusClient.getSessions(
      providerCode = providerCode,
      teamCode = teamCode,
      startDate = startDate,
      endDate = endDate,
      typeCode = projectTypeGroup?.let { projectTypeGroup ->
        projectService.projectTypesForGroup(projectTypeGroup).map { it.code }
      },
    ).toDto()
  }

  fun getSession(
    projectCode: String,
    date: LocalDate,
  ): SessionDto {
    val project = projectService.getProject(projectCode)
    val appointments = getSessionAppointments(projectCode, date)

    return sessionMappers.toSessionDto(
      date = date,
      project = project,
      appointments = appointments,
    )
  }

  fun allocateSupervisor(
    sessionId: SessionIdDto,
    supervisorCode: String,
    allocatedByUsername: String = contextService.getUserName(),
  ) {
    val existingAllocation = sessionSupervisorEntityRepository.findByIdOrNull(sessionId.toJpaId())

    sessionSupervisorEntityRepository.save(
      existingAllocation?.apply {
        this.supervisorCode = supervisorCode
        this.allocatedByUsername = allocatedByUsername
      }
        ?: SessionSupervisorEntity(
          projectCode = sessionId.projectCode,
          day = sessionId.day,
          supervisorCode = supervisorCode,
          allocatedByUsername = allocatedByUsername,
        ),
    )

    log.info("Session [$sessionId] allocated to [$supervisorCode]")
  }

  fun deallocateSupervisor(sessionId: SessionIdDto) {
    sessionSupervisorEntityRepository.deleteById(sessionId.toJpaId())

    log.info("Session [$sessionId] deallocated")
  }

  private fun SessionIdDto.toJpaId() = SessionSupervisorId(
    projectCode = projectCode,
    day = day,
  )

  fun getNextAllocationForSupervisor(supervisorCode: String): SessionSummaryDto? {
    val allocations = sessionSupervisorEntityRepository.findBySupervisorCodeAndDayGreaterThanEqualOrderByDayAsc(
      supervisorCode,
      LocalDate.now(),
    )

    return allocations.firstOrNull()?.toDto()
  }

  private fun SessionSupervisorEntity.toDto() = sessionMappers.toSummaryDto(
    date = day,
    project = projectService.getProject(projectCode),
    appointments = getSessionAppointments(projectCode, day),
  )

  private fun getSessionAppointments(
    projectCode: String,
    date: LocalDate,
  ) = appointmentService.getAppointments(
    fromDate = date,
    toDate = date,
    projectCodes = listOf(projectCode),
    pageable = PageRequest.of(0, Int.MAX_VALUE, Sort.by("name").descending()),
  ).content
}

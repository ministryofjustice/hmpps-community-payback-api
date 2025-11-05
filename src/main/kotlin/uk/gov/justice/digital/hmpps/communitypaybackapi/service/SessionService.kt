package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSessionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorId
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

@Service
class SessionService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
  val sessionMappers: SessionMappers,
  val sessionSupervisorEntityRepository: SessionSupervisorEntityRepository,
  val contextService: ContextService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getSessions(
    startDate: LocalDate,
    endDate: LocalDate,
    teamCode: String,
  ) = communityPaybackAndDeliusClient.getSessions(startDate, endDate, teamCode).toDto()

  fun getSession(
    projectCode: String,
    date: LocalDate,
  ): SessionDto {
    val projectSession = communityPaybackAndDeliusClient.getSession(projectCode, date)
    val caseSummaries = projectSession.appointmentSummaries.map { it.case }.toList()
    return sessionMappers.toDto(projectSession, offenderService.toOffenderInfos(caseSummaries))
  }

  fun allocateSupervisor(
    sessionId: SessionIdDto,
    supervisorCode: String,
  ) {
    sessionSupervisorEntityRepository.save(
      SessionSupervisorEntity(
        projectCode = sessionId.projectCode,
        day = sessionId.day,
        supervisorCode = supervisorCode,
        allocatedByUsername = contextService.getUserName(),
      ),
    )
    log.info("Session [$sessionId] allocated to [$supervisorCode]")
  }

  fun deallocateSupervisor(sessionId: SessionIdDto) {
    sessionSupervisorEntityRepository.deleteById(
      SessionSupervisorId(
        projectCode = sessionId.projectCode,
        day = sessionId.day,
      ),
    )

    log.info("Session [$sessionId] deallocated")
  }

  fun getFutureAllocationsForSupervisor(supervisorCode: String): SupervisorSessionsDto {
    val allocations = sessionSupervisorEntityRepository.findBySupervisorCodeAndDayGreaterThanEqualOrderByDayAsc(
      supervisorCode,
      LocalDate.now(),
    )

    return SupervisorSessionsDto(
      allocations.map { allocation ->
        val session = communityPaybackAndDeliusClient.getSession(
          allocation.projectCode,
          allocation.day,
        )

        sessionMappers.toSummaryDto(session)
      },
    )
  }
}

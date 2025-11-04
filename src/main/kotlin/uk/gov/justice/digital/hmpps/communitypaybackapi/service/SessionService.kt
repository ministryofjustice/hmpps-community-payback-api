package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalTime

@Service
class SessionService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
  val sessionMappers: SessionMappers,
  val sessionSupervisorEntityRepository: SessionSupervisorEntityRepository,
  val contextService: ContextService,
) {
  fun getSessions(
    startDate: LocalDate,
    endDate: LocalDate,
    teamCode: String,
  ) = communityPaybackAndDeliusClient.getSessions(startDate, endDate, teamCode).toDto()

  fun getSession(
    projectCode: String,
    date: LocalDate,
    start: LocalTime,
    end: LocalTime,
  ): SessionDto {
    val projectSession = communityPaybackAndDeliusClient.getSession(projectCode, date, start, end)
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
  }
}

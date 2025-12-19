package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
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
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val sessionMappers: SessionMappers,
  val sessionSupervisorEntityRepository: SessionSupervisorEntityRepository,
  val contextService: ContextService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SuppressWarnings("MagicNumber")
  fun getSessions(
    providerCode: String,
    teamCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
  ): SessionSummariesDto {
    if (ChronoUnit.DAYS.between(startDate, endDate) > 7) {
      throw BadRequestException("Date range cannot be greater than 7 days")
    }

    return communityPaybackAndDeliusClient.getSessions(providerCode, teamCode, startDate, endDate).toDto()
  }

  fun getSession(
    projectCode: String,
    date: LocalDate,
  ): SessionDto {
    val projectSession = communityPaybackAndDeliusClient.getSession(
      projectCode = projectCode,
      date = date,
      username = contextService.getUserName(),
    )
    return sessionMappers.toDto(
      date = date,
      session = projectSession,
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
    communityPaybackAndDeliusClient.getSession(
      projectCode = projectCode,
      date = day,
      username = contextService.getUserName(),
    ),
  )
}

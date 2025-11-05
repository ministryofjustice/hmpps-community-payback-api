package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.SessionMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate

@Service
class SessionService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
  val sessionMappers: SessionMappers,
) {
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
}

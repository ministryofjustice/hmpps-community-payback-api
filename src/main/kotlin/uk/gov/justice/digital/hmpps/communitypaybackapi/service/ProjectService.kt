package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ProjectMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import java.time.LocalDate
import java.time.LocalTime

@Service
class ProjectService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
  val projectMappers: ProjectMappers,
) {
  fun getProjectSessions(
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
    val projectSession = communityPaybackAndDeliusClient.getProjectSession(projectCode, date, start, end)
    val caseSummaries = projectSession.appointmentSummaries.map { it.case }.toList()
    return projectMappers.toDto(projectSession, offenderService.toOffenderInfos(caseSummaries))
  }
}

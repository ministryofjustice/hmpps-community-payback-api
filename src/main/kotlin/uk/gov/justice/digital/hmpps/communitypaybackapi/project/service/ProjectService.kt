package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto
import java.time.LocalDate
import java.time.LocalTime

@Service
class ProjectService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
) {
  fun getProjectSessions(
    startDate: LocalDate,
    endDate: LocalDate,
    teamCode: String,
  ) = communityPaybackAndDeliusClient.getProjectSessions(startDate, endDate, teamCode).toDto()

  fun getSession(
    projectCode: String,
    date: LocalDate,
    start: LocalTime,
    end: LocalTime,
  ): SessionDto {
    val projectSession = communityPaybackAndDeliusClient.getProjectSession(projectCode, date, start, end)
    val caseSummaries = projectSession.appointmentSummaries.map { it.case }.toList()
    return projectSession.toDto(offenderService.toOffenderInfos(caseSummaries))
  }
}

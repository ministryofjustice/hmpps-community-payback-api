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
  fun getProjectAllocations(
    startDate: LocalDate,
    endDate: LocalDate,
    teamId: Long, // TODO - should this be switched to code rather than delius id
  ) = communityPaybackAndDeliusClient.getProjectAllocations(startDate, endDate, teamId).toDto()

  fun getSession(
    projectCode: String,
    date: LocalDate,
    start: LocalTime,
    end: LocalTime,
  ): SessionDto {
    val projectSession = communityPaybackAndDeliusClient.getProjectSessions(projectCode, date, start, end)
    val crns = projectSession.appointmentSummaries.map { it.crn }.toSet()
    return projectSession.toDto(offenderService.getOffenderInfo(crns))
  }
}

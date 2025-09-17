package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentsDto
import java.time.LocalDate

@Service
class ProjectService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  val offenderService: OffenderService,
) {
  fun getProjectAllocations(
    startDate: LocalDate,
    endDate: LocalDate,
    teamId: Long,
  ) = communityPaybackAndDeliusClient.getProjectAllocations(startDate, endDate, teamId).toDto()

  fun getAppointments(
    projectId: Long,
    date: LocalDate,
  ): AppointmentsDto {
    val appointments = communityPaybackAndDeliusClient.getProjectAppointments(projectId, date)

    return appointments.toDto(
      offenderService.getOffenderInfo(appointments.appointments.map { it.crn }.toSet()),
    )
  }
}

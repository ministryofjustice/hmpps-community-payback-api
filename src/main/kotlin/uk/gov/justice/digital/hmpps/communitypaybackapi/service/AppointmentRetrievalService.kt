package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers

@Service
class AppointmentRetrievalService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val contextService: ContextService,
) {

  fun getAppointment(
    projectCode: String,
    appointmentId: Long,
  ): AppointmentDto = try {
    communityPaybackAndDeliusClient.getAppointment(
      projectCode = projectCode,
      appointmentId = appointmentId,
      username = contextService.getUserName(),
    ).let { appointmentMappers.toDto(it) }
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Appointment", "Project $projectCode, ID $appointmentId")
  }
}

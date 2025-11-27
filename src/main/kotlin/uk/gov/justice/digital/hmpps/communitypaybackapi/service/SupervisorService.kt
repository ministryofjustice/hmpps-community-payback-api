package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class SupervisorService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {

  fun getSupervisorInfo(username: String) = try {
    communityPaybackAndDeliusClient.getSupervisor(username).toDto()
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Supervisor", username)
  }
}

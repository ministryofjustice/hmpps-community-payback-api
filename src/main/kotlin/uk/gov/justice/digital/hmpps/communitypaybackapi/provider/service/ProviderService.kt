package uk.gov.justice.digital.hmpps.communitypaybackapi.provider.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ServiceResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ClientResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.callForClientResult

@Service
class ProviderService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProviders() = when (val response = callForClientResult { communityPaybackAndDeliusClient.providers() }) {
    is ClientResult.Success -> ServiceResult.Success(response.body.toDto())
    is ClientResult.Failure -> response.throwException()
  }

  fun getProviderTeams(providerId: Long) = when (val response = callForClientResult { communityPaybackAndDeliusClient.providerTeams(providerId) }) {
    is ClientResult.Success -> ServiceResult.Success(response.body.toDto())
    is ClientResult.Failure.HttpResponse if response.status == HttpStatus.NOT_FOUND -> ServiceResult.Error.NotFound("Provider", providerId.toString())
    is ClientResult.Failure -> response.throwException()
  }
}

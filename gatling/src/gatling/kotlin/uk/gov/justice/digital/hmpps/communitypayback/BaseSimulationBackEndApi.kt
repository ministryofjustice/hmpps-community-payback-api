package uk.gov.justice.digital.hmpps.communitypayback

import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import uk.gov.justice.digital.hmpps.communitypayback.config.HttpApiRequestConfig
import uk.gov.justice.digital.hmpps.communitypayback.config.OAuthTokenProvider

abstract class BaseSimulationBackEndApi(
  httpRequestConfig: HttpApiRequestConfig = HttpApiRequestConfig(),
  isSupervisor: Boolean = false,
) : Simulation() {
  protected val accessToken =
    OAuthTokenProvider.fetchAccessToken(isSupervisor) ?: throw IllegalStateException("No access token available")

  protected val supervisorAccessToken =
    if (isSupervisor) accessToken else OAuthTokenProvider.fetchAccessToken(true) ?: throw IllegalStateException("No supervisor access token available")

  protected val httpProtocol =
    http.baseUrl(httpRequestConfig.apiUrl)
      .acceptHeader("*/*")
      .contentTypeHeader("application/json")
      .authorizationHeader("Bearer $accessToken")

  protected val supervisorHttpProtocol =
    http.baseUrl(httpRequestConfig.apiUrl)
      .acceptHeader("*/*")
      .contentTypeHeader("application/json")
      .authorizationHeader("Bearer $supervisorAccessToken")
}

package uk.gov.justice.digital.hmpps.communitypayback

import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import uk.gov.justice.digital.hmpps.communitypayback.config.HttpRequestConfig

open class BaseSimulationFrontEndRoutes(httpRequestConfig: HttpRequestConfig = HttpRequestConfig()) : Simulation() {
    protected val httpProtocol =
        http.baseUrl("${httpRequestConfig.protocol}://${httpRequestConfig.domain}")
            .acceptHeader(httpRequestConfig.acceptHeader)
            .acceptLanguageHeader(httpRequestConfig.acceptLanguageHeader)
            .acceptEncodingHeader(httpRequestConfig.acceptEncodingHeader)
            .userAgentHeader(httpRequestConfig.userAgentHeader)

}
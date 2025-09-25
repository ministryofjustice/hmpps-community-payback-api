package uk.gov.justice.digital.hmpps.communitypayback

import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import uk.gov.justice.digital.hmpps.communitypayback.config.HttpApiRequestConfig

open class BaseSimulationBackEndApi(httpRequestConfig: HttpApiRequestConfig = HttpApiRequestConfig()) : Simulation() {
    protected val httpProtocol =
        http.baseUrl("${httpRequestConfig.protocol}://${httpRequestConfig.domain}:${httpRequestConfig.port}")
            .acceptHeader("*/*")
            .contentTypeHeader("application/json")
            .authorizationHeader("Bearer ${httpRequestConfig.jwt}")

}
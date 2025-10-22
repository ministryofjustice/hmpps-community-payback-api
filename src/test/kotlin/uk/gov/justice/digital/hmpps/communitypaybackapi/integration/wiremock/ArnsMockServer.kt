package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AllRoshRisk

object ArnsMockServer {

  val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun rosh(crn: String, allRoshRisk: AllRoshRisk) {
    WireMock.stubFor(
      get("/arns/risks/rosh/$crn")
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(allRoshRisk)),
        ),
    )
  }

  fun roshNotFound(crn: String) {
    WireMock.stubFor(
      get("/arns/risks/rosh/$crn")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }
}

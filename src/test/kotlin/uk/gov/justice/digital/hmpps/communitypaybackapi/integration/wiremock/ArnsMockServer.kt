package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AllRoshRisk

object ArnsMockServer {

  val jsonMapper: JsonMapper = JsonMapper()

  fun rosh(crn: String, allRoshRisk: AllRoshRisk) {
    WireMock.stubFor(
      get("/arns/risks/rosh/$crn")
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(allRoshRisk)),
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

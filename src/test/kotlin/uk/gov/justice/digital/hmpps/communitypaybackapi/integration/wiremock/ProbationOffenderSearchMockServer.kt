package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.IDs
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderDetail

object ProbationOffenderSearchMockServer {

  private val jsonMapper = JsonMapper()

  fun stubSingleMatch(crn: String) {
    stubFor(
      post(urlEqualTo("/probation-offender-search/search"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              jsonMapper.writeValueAsString(
                listOf(
                  OffenderDetail(
                    otherIds = IDs(crn = crn),
                  ),
                ),
              ),
            ),
        ),
    )
  }

  fun stubNoMatches() {
    stubFor(
      post(urlEqualTo("/probation-offender-search/search"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(emptyList<OffenderDetail>())),
        ),
    )
  }

  fun stubSearchError() {
    stubFor(
      post(urlEqualTo("/probation-offender-search/search"))
        .willReturn(serverError()),
    )
  }

  fun stubMultipleMatches(vararg crns: String) {
    stubFor(
      post(urlEqualTo("/probation-offender-search/search"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              jsonMapper.writeValueAsString(
                crns.map { crn ->
                  OffenderDetail(
                    otherIds = IDs(crn = crn),
                  )
                },
              ),
            ),
        ),
    )
  }
}

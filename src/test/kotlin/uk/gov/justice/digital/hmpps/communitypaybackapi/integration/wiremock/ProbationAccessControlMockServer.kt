package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccessItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid

object ProbationAccessControlMockServer {

  val jsonMapper = JsonMapper()

  fun setupGetAccessControlForCrnsDefault() {
    stubFor(
      post(urlMatching("/probation-access-control/user/.*/access"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(NDCaseAccess.valid())),
        ),
    )
  }

  fun setupGetAccessControlForCrnsResponse(username: String, vararg response: NDCaseAccessItem) {
    stubFor(
      post("/probation-access-control/user/$username/access")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(NDCaseAccess.valid(*response))),
        ),
    )
  }
}

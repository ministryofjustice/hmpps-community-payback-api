package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.health

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ArnsMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ProbationAccessControlMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ProbationOffenderSearchMockServer

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "arns",
      "communityPaybackAndDelius",
      "hmppsAuth",
      "probationAccessControl",
      "probationOffenderSearch",
    ],
  )
  fun `Health page reports down when downstream service is down`(downService: String) {
    stubPingWithResponse(200)
    when (downService) {
      "arns" -> ArnsMockServer.stubHealthPing(503)
      "communityPaybackAndDelius" -> CommunityPaybackAndDeliusMockServer.stubHealthPing(503)
      "hmppsAuth" -> HmppsAuthMockServer.stubHealthPing(503)
      "probationAccessControl" -> ProbationAccessControlMockServer.stubHealthPing(503)
      "probationOffenderSearch" -> ProbationOffenderSearchMockServer.stubHealthPing(503)
    }

    val expectedStatusesByService = arrayOf(
      "arns",
      "communityPaybackAndDelius",
      "hmppsAuth",
      "probationAccessControl",
      "probationOffenderSearch",
    ).associateWith { service -> if (service == downService) "DOWN" else "UP" }

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .let {
        for ((service, expectedStatus) in expectedStatusesByService) {
          it.jsonPath("components.$service.status").isEqualTo(expectedStatus)
        }
      }
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}

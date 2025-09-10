package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.provider.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummariesDto

class ProviderTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /providers")
  inner class ProviderEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/providers")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/providers")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/providers")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      val providers = webTestClient.get()
        .uri("/providers")
        .headers(
          setAuthorisation(
            username = "INTEGRATION_TEST",
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummariesDto>()

      assertThat(providers.providers).hasSize(3)
    }
  }
}

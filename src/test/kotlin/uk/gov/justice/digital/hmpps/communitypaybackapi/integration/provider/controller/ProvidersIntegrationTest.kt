package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.provider.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.controller.ProviderSummariesDto

class ProvidersIntegrationTest : IntegrationTestBase() {

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
      CommunityPaybackAndDeliusMockServer.providers(
        ProviderSummaries(
          listOf(
            ProviderSummary(1, "Entry 1"),
            ProviderSummary(2, "Entry 2"),
            ProviderSummary(3, "Entry 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/providers")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummariesDto>()

      assertThat(providers.providers).hasSize(3)
      assertThat(providers.providers[0].id).isEqualTo(1L)
      assertThat(providers.providers[0].name).isEqualTo("Entry 1")
    }
  }

  @Nested
  @DisplayName("GET /providers/123/teams")
  inner class ProviderTeamsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/providers/123/teams")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/providers/123/teams")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/providers/123/teams")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      CommunityPaybackAndDeliusMockServer.providerTeams(
        ProviderTeamSummaries(
          listOf(
            ProviderTeamSummary(11, "Team 1"),
            ProviderTeamSummary(12, "Team 2"),
            ProviderTeamSummary(13, "Team 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/providers/123/teams")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummariesDto>()

      assertThat(providers.providers).hasSize(3)
      assertThat(providers.providers[0].id).isEqualTo(11L)
      assertThat(providers.providers[0].name).isEqualTo("Team 1")
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class CommonProvidersIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /common/providers")
  inner class ProviderEndpoint {

    @Test
    fun `should return bad request if username is missing`() {
      webTestClient.get()
        .uri("/common/providers")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/providers")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/providers?username=doesntmatter")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/providers?username=doesntmatter")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI", "ROLE_COMMUNITY_PAYBACK__SUPERVISOR"])
    fun `should return OK`(role: String) {
      CommunityPaybackAndDeliusMockServer.setupGetProvidersResponse(
        username = "calling_user",
        providers = NDProviderSummaries(
          listOf(
            NDProviderSummary(code = "ABC123", "Entry 1"),
            NDProviderSummary(code = "DEF123", "Entry 2"),
            NDProviderSummary(code = "GHI123", "Entry 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/common/providers?username=calling_user")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummariesDto>()

      assertThat(providers.providers).hasSize(3)
      assertThat(providers.providers[0].code).isEqualTo("ABC123")
      assertThat(providers.providers[0].name).isEqualTo("Entry 1")
    }
  }

  @Nested
  @DisplayName("GET /common/providers/{providerCode}/teams")
  inner class ProviderTeamsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/providers/123/teams")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/providers/123/teams")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/providers/123/teams")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI", "ROLE_COMMUNITY_PAYBACK__SUPERVISOR"])
    fun `should return OK`(role: String) {
      CommunityPaybackAndDeliusMockServer.setupGetProviderTeamsResponse(
        providerCode = "N123456789",
        NDProviderTeamSummaries(
          listOf(
            NDProviderTeamSummary(code = "ABC123", "Team 1"),
            NDProviderTeamSummary(code = "DEF123", "Team 2"),
            NDProviderTeamSummary(code = "GHI123", "Team 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/common/providers/N123456789/teams")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderTeamSummariesDto>()

      assertThat(providers.providers).hasSize(3)
      assertThat(providers.providers[0].code).isEqualTo("ABC123")
      assertThat(providers.providers[0].name).isEqualTo("Team 1")
    }
  }
}

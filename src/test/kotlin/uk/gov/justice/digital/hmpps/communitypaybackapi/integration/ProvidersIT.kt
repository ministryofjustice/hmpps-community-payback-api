package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.provider.dto.SupervisorSummariesDto

class ProvidersIT : IntegrationTestBase() {

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
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummariesDto>()

      Assertions.assertThat(providers.providers).hasSize(3)
      Assertions.assertThat(providers.providers[0].id).isEqualTo(1L)
      Assertions.assertThat(providers.providers[0].name).isEqualTo("Entry 1")
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
        providerId = 123,
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
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderTeamSummariesDto>()

      Assertions.assertThat(providers.providers).hasSize(3)
      Assertions.assertThat(providers.providers[0].id).isEqualTo(11L)
      Assertions.assertThat(providers.providers[0].name).isEqualTo("Team 1")
    }
  }

  @Nested
  @DisplayName("GET /providers/{providerId}/teams/{teamId}/supervisors")
  inner class TeamSupervisorsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/providers/123/teams/99/supervisors")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/providers/123/teams/99/supervisors")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with team supervisors`() {
      CommunityPaybackAndDeliusMockServer.teamSupervisors(
        SupervisorSummaries(
          listOf(
            SupervisorSummary(id = 4L, name = "Fred Flintstone"),
            SupervisorSummary(id = 5L, name = "Barney Rubble"),
          ),
        ),
      )

      val supervisors = webTestClient.get()
        .uri("/providers/123/teams/99/supervisors")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SupervisorSummariesDto>()

      Assertions.assertThat(supervisors.supervisors).hasSize(2)
      Assertions.assertThat(supervisors.supervisors[0].id).isEqualTo(4L)
      Assertions.assertThat(supervisors.supervisors[0].name).isEqualTo("Fred Flintstone")
      Assertions.assertThat(supervisors.supervisors[1].id).isEqualTo(5L)
      Assertions.assertThat(supervisors.supervisors[1].name).isEqualTo("Barney Rubble")
    }

    @Test
    fun `should return empty list when no supervisors found`() {
      CommunityPaybackAndDeliusMockServer.teamSupervisors(
        SupervisorSummaries(emptyList()),
      )

      val supervisors = webTestClient.get()
        .uri("/providers/123/teams/99/supervisors")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SupervisorSummariesDto>()

      Assertions.assertThat(supervisors.supervisors).isEmpty()
    }

    @Test
    fun `should return 404 when no team found`() {
      CommunityPaybackAndDeliusMockServer.teamSupervisorsNotFound()

      webTestClient.get()
        .uri("/providers/666/teams/66/supervisors")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}

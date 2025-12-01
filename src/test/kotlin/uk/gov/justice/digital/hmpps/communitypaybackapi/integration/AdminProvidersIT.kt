package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderTeamSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate

class AdminProvidersIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /admin/providers")
  inner class ProviderEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/providers")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/providers?username=doesntmatter")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/providers?username=doesntmatter")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      CommunityPaybackAndDeliusMockServer.providers(
        username = "calling_user",
        providers = ProviderSummaries(
          listOf(
            ProviderSummary(code = "ABC123", "Entry 1"),
            ProviderSummary(code = "DEF123", "Entry 2"),
            ProviderSummary(code = "GHI123", "Entry 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/admin/providers?username=calling_user")
        .addAdminUiAuthHeader()
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
  @DisplayName("GET /admin/providers/123/teams")
  inner class ProviderTeamsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/providers/123/teams")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/providers/123/teams")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/providers/123/teams")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      CommunityPaybackAndDeliusMockServer.providerTeams(
        providerCode = "N123456789",
        ProviderTeamSummaries(
          listOf(
            ProviderTeamSummary(code = "ABC123", "Team 1"),
            ProviderTeamSummary(code = "DEF123", "Team 2"),
            ProviderTeamSummary(code = "GHI123", "Team 3"),
          ),
        ),
      )

      val providers = webTestClient.get()
        .uri("/admin/providers/N123456789/teams")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderTeamSummariesDto>()

      assertThat(providers.providers).hasSize(3)
      assertThat(providers.providers[0].code).isEqualTo("ABC123")
      assertThat(providers.providers[0].name).isEqualTo("Team 1")
    }
  }

  @Nested
  @DisplayName("GET /admin/providers/{providerCode}/teams/{teamCode}/supervisors")
  inner class TeamSupervisorsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/providers/123/teams/99/supervisors")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/providers/123/teams/99/supervisors")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with team supervisors`() {
      CommunityPaybackAndDeliusMockServer.teamSupervisors(
        SupervisorSummaries(
          listOf(SupervisorSummary.valid(), SupervisorSummary.valid()),
        ),
      )

      val supervisors = webTestClient.get()
        .uri("/admin/providers/123/teams/99/supervisors")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SupervisorSummariesDto>()

      assertThat(supervisors.supervisors).hasSize(2)
    }

    @Test
    fun `should return empty list when no supervisors found`() {
      CommunityPaybackAndDeliusMockServer.teamSupervisors(
        SupervisorSummaries(emptyList()),
      )

      val supervisors = webTestClient.get()
        .uri("/admin/providers/123/teams/99/supervisors")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SupervisorSummariesDto>()

      assertThat(supervisors.supervisors).isEmpty()
    }
  }

  @Nested
  @DisplayName("GET /admin/providers/{providerCode}/teams/{teamCode}/sessions")
  inner class SessionSearchEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/providers/PC01/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/providers/PC01/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/providers/PC01/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/admin/providers/PC01/teams/1/sessions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session summaries`() {
      CommunityPaybackAndDeliusMockServer.getSessions(
        providerCode = "PC01",
        teamCode = "999",
        startDate = LocalDate.of(2025, 1, 9),
        endDate = LocalDate.of(2025, 1, 12),
        projectSessions = SessionSummaries(
          listOf(
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Community Garden Maintenance")),
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Park Cleanup")),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/admin/providers/PC01/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-12")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(sessionSearchResults.allocations).hasSize(2)
      assertThat(sessionSearchResults.allocations[0].projectName).isEqualTo("Community Garden Maintenance")
      assertThat(sessionSearchResults.allocations[1].projectName).isEqualTo("Park Cleanup")
    }

    @Test
    fun `should return empty list when no session summaries found`() {
      CommunityPaybackAndDeliusMockServer.getSessions(
        providerCode = "PC01",
        teamCode = "999",
        startDate = LocalDate.of(2025, 1, 9),
        endDate = LocalDate.of(2025, 1, 11),
        projectSessions = SessionSummaries(emptyList()),
      )

      val sessionSummaries = webTestClient.get()
        .uri("/admin/providers/PC01/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-11")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(sessionSummaries.allocations).isEmpty()
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse.PageMeta
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.empty
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate

class CommonTeamIT : IntegrationTestBase() {
  @Nested
  @DisplayName("GET /common/teams/{teamCode}/sessions")
  inner class SessionSearchEndpoint {
    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/teams/1/sessions?startDate=2025-09-01&endDate=2025-09-07")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/common/teams/1/sessions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session summaries`() {
      val projectName1 = "Community Garden Maintenance"
      val projectName2 = "Park Cleanup"
      val sessions = listOf(
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName1)),
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName2)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
        listOf("999"),
        LocalDate.of(2025, 1, 9),
        LocalDate.of(2025, 1, 12),
        sessions = PageResponse(
          content = sessions,
          page = PageMeta(50, 0, 2, 1),
        ),
        listOf("NP1", "NP2", "PL"),
        "projectName,asc",
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/common/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-12")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<SessionSummaryDto>>()

      assertThat(sessionSearchResults.content).hasSize(2)
      assertThat(sessionSearchResults.content[0].projectName).isEqualTo(projectName1)
      assertThat(sessionSearchResults.content[1].projectName).isEqualTo(projectName2)
      assertThat(sessionSearchResults.page.size).isEqualTo(50)
      assertThat(sessionSearchResults.page.totalPages).isEqualTo(1)
      assertThat(sessionSearchResults.page.totalElements).isEqualTo(2)
      assertThat(sessionSearchResults.page.number).isEqualTo(0)
    }

    @Test
    fun `should return OK with project session summaries for provided project type codes`() {
      val projectName1 = "Community Garden Maintenance"
      val projectName2 = "Park Cleanup"
      val sessions = listOf(
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName1)),
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName2)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
        listOf("999"),
        LocalDate.of(2025, 1, 9),
        LocalDate.of(2025, 1, 12),
        sessions = PageResponse(
          content = sessions,
          page = PageMeta(50, 0, 2, 1),
        ),
        listOf("NP1"),
        "projectName,asc",
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/common/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-12&projectType=NP1")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<SessionSummaryDto>>()

      assertThat(sessionSearchResults.content).hasSize(2)
      assertThat(sessionSearchResults.content[0].projectName).isEqualTo(projectName1)
      assertThat(sessionSearchResults.content[1].projectName).isEqualTo(projectName2)
      assertThat(sessionSearchResults.page.size).isEqualTo(50)
      assertThat(sessionSearchResults.page.totalPages).isEqualTo(1)
      assertThat(sessionSearchResults.page.totalElements).isEqualTo(2)
      assertThat(sessionSearchResults.page.number).isEqualTo(0)
    }

    @Test
    fun `should return OK with paginated response when pagination parameters are provided`() {
      val projectName1 = "Community Garden Maintenance"
      val projectName2 = "Park Cleanup"
      val sessions = listOf(
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName1)),
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName2)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
        listOf("999"),
        LocalDate.of(2025, 1, 9),
        LocalDate.of(2025, 1, 12),
        sessions = PageResponse(
          content = sessions,
          page = PageMeta(25, 0, 2, 1),
        ),
        listOf("NP1", "NP2", "PL"),
        "projectName,desc",
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/common/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-12&page=0&size=25&sort=projectName,desc")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<SessionSummaryDto>>()

      assertThat(sessionSearchResults.content).hasSize(2)
      assertThat(sessionSearchResults.content[0].projectName).isEqualTo(projectName1)
      assertThat(sessionSearchResults.content[1].projectName).isEqualTo(projectName2)
      assertThat(sessionSearchResults.page.size).isEqualTo(25)
      assertThat(sessionSearchResults.page.totalPages).isEqualTo(1)
      assertThat(sessionSearchResults.page.totalElements).isEqualTo(2)
      assertThat(sessionSearchResults.page.number).isEqualTo(0)
    }

    @Test
    fun `should return empty list when no session summaries found`() {
      CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
        listOf(element = "999"),
        startDate = LocalDate.of(2025, 1, 9),
        endDate = LocalDate.of(2025, 1, 11),
        sessions = PageResponse.empty(),
        typeCode = listOf("NP1", "NP2", "PL"),
        sortString = "projectName,asc",
      )

      val sessionSummaries = webTestClient.get()
        .uri("/common/teams/999/sessions?startDate=2025-01-09&endDate=2025-01-11")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<SessionSummaryDto>>()

      assertThat(sessionSummaries.content).isEmpty()
    }
  }
}

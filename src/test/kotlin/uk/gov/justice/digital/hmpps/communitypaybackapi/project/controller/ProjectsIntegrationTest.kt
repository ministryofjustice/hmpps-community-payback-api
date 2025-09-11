package uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate

class ProjectsIntegrationTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /projects/allocations")
  inner class ProjectAllocationsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/projects/allocations")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
        .exchange()
        .expectStatus()
        .is5xxServerError
    }

    @Test
    fun `should return OK with project allocations`() {
      CommunityPaybackAndDeliusMockServer.projectAllocations(
        ProjectAllocations(
          listOf(
            ProjectAllocation(
              id = 1L,
              projectName = "Community Garden Maintenance",
              teamId = 1L,
              startDate = LocalDate.of(2025, 9, 1),
              endDate = LocalDate.of(2025, 9, 7),
              hours = 40,
            ),
            ProjectAllocation(
              id = 2L,
              projectName = "Park Cleanup",
              teamId = 1L,
              startDate = LocalDate.of(2025, 9, 8),
              endDate = LocalDate.of(2025, 9, 14),
              hours = 32,
            ),
          ),
        ),
      )

      val allocations = webTestClient.get()
        .uri("/projects/allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAllocationsDto>()

      assertThat(allocations.allocations).hasSize(2)
      assertThat(allocations.allocations[0].id).isEqualTo(1L)
      assertThat(allocations.allocations[0].projectName).isEqualTo("Community Garden Maintenance")
      assertThat(allocations.allocations[0].teamId).isEqualTo(1L)
      assertThat(allocations.allocations[0].startDate).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(allocations.allocations[0].endDate).isEqualTo(LocalDate.of(2025, 9, 7))
      assertThat(allocations.allocations[0].hours).isEqualTo(40)

      assertThat(allocations.allocations[1].id).isEqualTo(2L)
      assertThat(allocations.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(allocations.allocations[1].teamId).isEqualTo(1L)
      assertThat(allocations.allocations[1].startDate).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(allocations.allocations[1].endDate).isEqualTo(LocalDate.of(2025, 9, 14))
      assertThat(allocations.allocations[1].hours).isEqualTo(32)
    }

    @Test
    fun `should return empty list when no allocations found`() {
      CommunityPaybackAndDeliusMockServer.projectAllocations(
        ProjectAllocations(emptyList()),
      )

      val allocations = webTestClient.get()
        .uri("/projects/allocations?startDate=01/09/2025&endDate=07/09/2025&teamId=999")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAllocationsDto>()

      assertThat(allocations.allocations).isEmpty()
    }
  }
}

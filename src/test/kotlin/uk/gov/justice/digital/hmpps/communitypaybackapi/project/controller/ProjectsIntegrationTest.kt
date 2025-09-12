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
import java.time.LocalTime

class ProjectsIntegrationTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /projects/allocations")
  inner class ProjectAllocationsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
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
              date = LocalDate.of(2025, 9, 1),
              startTime = LocalTime.of(9, 0),
              endTime = LocalTime.of(17, 0),
              projectCode = "cgm",
              numberOfOffendersAllocated = 0,
              numberOfOffendersWithOutcomes = 1,
              numberOfOffendersWithEA = 2,
            ),
            ProjectAllocation(
              id = 2L,
              projectName = "Park Cleanup",
              date = LocalDate.of(2025, 9, 8),
              startTime = LocalTime.of(8, 0),
              endTime = LocalTime.of(16, 0),
              projectCode = "pc",
              numberOfOffendersAllocated = 3,
              numberOfOffendersWithOutcomes = 4,
              numberOfOffendersWithEA = 5,
            ),
          ),
        ),
      )

      val allocations = webTestClient.get()
        .uri("/projects/allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
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
      assertThat(allocations.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(allocations.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(allocations.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(allocations.allocations[0].projectCode).isEqualTo("cgm")
      assertThat(allocations.allocations[0].numberOfOffendersAllocated).isEqualTo(0)
      assertThat(allocations.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(1)
      assertThat(allocations.allocations[0].numberOfOffendersWithEA).isEqualTo(2)
    }

    @Test
    fun `should return empty list when no allocations found`() {
      CommunityPaybackAndDeliusMockServer.projectAllocations(
        ProjectAllocations(emptyList()),
      )

      val allocations = webTestClient.get()
        .uri("/projects/allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.controller.ProjectTypesDto

class ReferencesIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /references/project-types")
  inner class ProjectTypesEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/references/project-types")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/references/project-types")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/references/project-types")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with project types`() {
      CommunityPaybackAndDeliusMockServer.projectTypes(
        ProjectTypes(
          listOf(
            ProjectType(
              id = 1234,
              name = "Community Garden Maintenance",
            ),
            ProjectType(
              id = 5678,
              name = "Park Cleanup",
            ),
            ProjectType(
              id = 9012,
              name = "Library Assistance",
            ),
          ),
        ),
      )

      val projectTypes = webTestClient.get()
        .uri("/references/project-types")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectTypesDto>()

      assertThat(projectTypes.projectTypes).hasSize(3)
      assertThat(projectTypes.projectTypes[0].id).isEqualTo(1234)
      assertThat(projectTypes.projectTypes[0].name).isEqualTo("Community Garden Maintenance")
      assertThat(projectTypes.projectTypes[1].id).isEqualTo(5678)
      assertThat(projectTypes.projectTypes[1].name).isEqualTo("Park Cleanup")
      assertThat(projectTypes.projectTypes[2].id).isEqualTo(9012)
      assertThat(projectTypes.projectTypes[2].name).isEqualTo("Library Assistance")
    }

    @Test
    fun `should return empty list when no project types found`() {
      CommunityPaybackAndDeliusMockServer.projectTypes(
        ProjectTypes(emptyList()),
      )

      val projectTypes = webTestClient.get()
        .uri("/references/project-types")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectTypesDto>()

      Assertions.assertThat(projectTypes.projectTypes).isEmpty()
    }
  }
}

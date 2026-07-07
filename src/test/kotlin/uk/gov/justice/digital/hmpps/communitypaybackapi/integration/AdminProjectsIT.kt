package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalTime

class AdminProjectsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /admin/projects")
  inner class ProjectsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/projects/PROJ1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/projects/PROJ1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/projects/PROJ1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if a project can't be found`() {
      CommunityPaybackAndDeliusMockServer.setupGetProject404Response(
        projectCode = "PC01",
      )

      webTestClient.get()
        .uri("/admin/projects/PC01")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNotFound()
    }

    @Test
    fun `Should return existing project`() {
      val availability = NDProjectAvailability.valid().copy(
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(16, 45),
      )

      CommunityPaybackAndDeliusMockServer.setupGetProjectResponse(
        project = NDProject.valid(ctx).copy(
          code = "PC01",
          name = "the project name",
          availability = listOf(availability),
        ),
      )

      val response = webTestClient.get()
        .uri("/admin/projects/PC01")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<ProjectDto>()

      assertThat(response.projectName).isEqualTo("the project name")
      assertThat(response.availability).hasSize(1)
      assertThat(response.availability[0].startTime).isEqualTo(LocalTime.of(9, 30))
      assertThat(response.availability[0].endTime).isEqualTo(LocalTime.of(16, 45))
    }
  }
}

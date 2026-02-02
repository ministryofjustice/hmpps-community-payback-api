package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class SupervisorSupervisorsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /supervisor/supervisors")
  inner class GetSupervisorEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/supervisors?username=thesupervisorusername")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/supervisors?username=thesupervisorusername")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/supervisors?username=thesupervisorusername")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/supervisor/supervisors")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return 404 if supervisor cant be found`() {
      CommunityPaybackAndDeliusMockServer.getSupervisorNotFound(
        username = "thesupervisorusername",
      )

      webTestClient.get()
        .uri("/supervisor/supervisors?username=thesupervisorusername")
        .addSupervisorUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return OK with supervisor info`() {
      CommunityPaybackAndDeliusMockServer.getSupervisor(
        username = "thesupervisorusername",
        supervisor = NDSupervisor.valid().copy(
          code = "SUP01",
          isUnpaidWorkTeamMember = true,
        ),
      )

      val supervisorResult = webTestClient.get()
        .uri("/supervisor/supervisors?username=thesupervisorusername")
        .addSupervisorUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SupervisorDto>()

      assertThat(supervisorResult.code).isEqualTo("SUP01")
      assertThat(supervisorResult.isUnpaidWorkTeamMember).isTrue
    }
  }
}

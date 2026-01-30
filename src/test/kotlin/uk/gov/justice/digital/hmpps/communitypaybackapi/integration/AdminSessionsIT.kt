package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AllocateSupervisorToSessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate

class AdminSessionsIT : IntegrationTestBase() {

  @Autowired
  lateinit var sessionSupervisorEntityRepository: SessionSupervisorEntityRepository

  @Nested
  @DisplayName("GET /admin/projects/123/sessions/2025-01-09")
  inner class GetSessionEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with project session`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        username = "USER1",
        date = LocalDate.of(2025, 1, 9),
        session = NDSession.valid().copy(
          project = NDProject.valid().copy(
            name = "Community Garden Maintenance",
            code = "N123456789",
          ),
          appointmentSummaries = listOf(
            NDAppointmentSummary.valid().copy(outcome = null),
            NDAppointmentSummary.valid().copy(outcome = null),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/admin/projects/N123456789/sessions/2025-01-09")
        .addAdminUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(sessionSearchResults.projectName).isEqualTo("Community Garden Maintenance")
      assertThat(sessionSearchResults.projectCode).isEqualTo("N123456789")
      assertThat(sessionSearchResults.date).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(sessionSearchResults.appointmentSummaries).hasSize(2)
    }
  }

  @Nested
  @DisplayName("POST /admin/projects/123/sessions/2025-01-09/supervisor")
  inner class AllocateSupervisorEndpoint {

    @Test
    fun clearAllocations() {
      sessionSupervisorEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .bodyValue(AllocateSupervisorToSessionDto("s1"))
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .bodyValue(AllocateSupervisorToSessionDto("s1"))
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `allocate session`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .bodyValue(AllocateSupervisorToSessionDto("supervisor_code_1"))
        .exchange()
        .expectStatus()
        .isOk

      val allocation = sessionSupervisorEntityRepository.findAll().first()
      assertThat(allocation.projectCode).isEqualTo("123")
      assertThat(allocation.day).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(allocation.supervisorCode).isEqualTo("supervisor_code_1")
      assertThat(allocation.allocatedByUsername).isEqualTo("ADMIN_USER1")
    }

    @Test
    fun `reallocate session`() {
      sessionSupervisorEntityRepository.save(
        SessionSupervisorEntity(
          projectCode = "123",
          day = LocalDate.of(2025, 1, 9),
          supervisorCode = "OTHER_USER",
          allocatedByUsername = "OTHER_ALLOCATOR",
        ),
      )

      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .bodyValue(AllocateSupervisorToSessionDto("supervisor_code_1"))
        .exchange()
        .expectStatus()
        .isOk

      val allocation = sessionSupervisorEntityRepository.findAll().first()
      assertThat(allocation.projectCode).isEqualTo("123")
      assertThat(allocation.day).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(allocation.supervisorCode).isEqualTo("supervisor_code_1")
      assertThat(allocation.allocatedByUsername).isEqualTo("ADMIN_USER1")
      assertThat(allocation.createdAt).isNotEqualTo(allocation.updatedAt)
    }
  }

  @Nested
  @DisplayName("DELETE /admin/projects/123/sessions/2025-01-09/supervisor")
  inner class DeallocateSupervisorEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `delete existing allocation`() {
      sessionSupervisorEntityRepository.deleteAll()

      sessionSupervisorEntityRepository.save(
        SessionSupervisorEntity(
          projectCode = "123",
          day = LocalDate.of(2025, 1, 9),
          supervisorCode = "super1",
          allocatedByUsername = "user1",
        ),
      )

      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .exchange()
        .expectStatus()
        .isOk

      assertThat(sessionSupervisorEntityRepository.count()).isEqualTo(0)
    }

    @Test
    fun `delete succeeds if no allocation exists`() {
      sessionSupervisorEntityRepository.deleteAll()

      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .exchange()
        .expectStatus()
        .isOk
    }
  }
}

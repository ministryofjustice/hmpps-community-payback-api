package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AssignSupervisorDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import java.time.LocalDate

class AdminSessionsIT : IntegrationTestBase() {

  @Autowired
  lateinit var sessionSupervisorEntityRepository: SessionSupervisorEntityRepository

  @BeforeEach
  fun clearAssignments() {
    sessionSupervisorEntityRepository.deleteAll()
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `assigning the same supervisor twice through the endpoint is idempotent`(projectWide: Boolean) {
    val date = if (projectWide) null else LocalDate.of(2025, 1, 9)
    val uri = if (projectWide) "/admin/projects/123/supervisor" else "/admin/projects/123/sessions/$date/supervisor"

    webTestClient.post()
      .uri(uri)
      .addAdminUiAuthHeader(username = "ADMIN_USER1")
      .bodyValue(AssignSupervisorDto("s1"))
      .exchange()
      .expectStatus().isOk

    val original = sessionSupervisorEntityRepository.findAll().single()

    webTestClient.post()
      .uri(uri)
      .addAdminUiAuthHeader(username = "ADMIN_USER2")
      .bodyValue(AssignSupervisorDto("s1"))
      .exchange()
      .expectStatus().isOk

    val assignments = sessionSupervisorEntityRepository.findAll()
    assertThat(assignments).hasSize(1)
    val assignment = assignments.single()
    assertThat(assignment.id).isEqualTo(original.id)
    assertThat(assignment.projectCode).isEqualTo("123")
    assertThat(assignment.day).isEqualTo(date)
    assertThat(assignment.supervisorCode).isEqualTo("s1")
    assertThat(assignment.assignedByUsername).isEqualTo("ADMIN_USER2")
  }

  @Test
  fun `project assignments allow multiple supervisors and repeated requests are idempotent`() {
    listOf("s1", "s2", "s1").forEach { code ->
      webTestClient.post()
        .uri("/admin/projects/123/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .bodyValue(AssignSupervisorDto(code))
        .exchange()
        .expectStatus().isOk
    }
    val assignments = sessionSupervisorEntityRepository.findAll()
    assertThat(assignments.map { it.supervisorCode }).containsExactlyInAnyOrder("s1", "s2")
    assertThat(assignments.map { it.day }).containsOnlyNulls()
    assertThat(sessionSupervisorEntityRepository.findAssignments("s1", LocalDate.now()))
      .hasSize(1)

    webTestClient.post()
      .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
      .addAdminUiAuthHeader(username = "ADMIN_USER1")
      .bodyValue(AssignSupervisorDto("s1"))
      .exchange()
      .expectStatus().isOk

    webTestClient.delete()
      .uri("/admin/projects/123/supervisor/s1")
      .addAdminUiAuthHeader(username = "ADMIN_USER1")
      .exchange()
      .expectStatus().isOk
    val remaining = sessionSupervisorEntityRepository.findAll()
    assertThat(remaining.map { it.supervisorCode }).containsExactlyInAnyOrder("s1", "s2")
    assertThat(remaining.single { it.supervisorCode == "s1" }.day).isEqualTo(LocalDate.of(2025, 1, 9))
  }

  @Nested
  @DisplayName("POST /admin/projects/123/sessions/2025-01-09/supervisor")
  inner class AssignSupervisorEndpoint {

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
        .bodyValue(AssignSupervisorDto("s1"))
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .bodyValue(AssignSupervisorDto("s1"))
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `assign session`() {
      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .bodyValue(AssignSupervisorDto("supervisor_code_1"))
        .exchange()
        .expectStatus()
        .isOk

      val assignment = sessionSupervisorEntityRepository.findAll().first()
      assertThat(assignment.projectCode).isEqualTo("123")
      assertThat(assignment.day).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(assignment.supervisorCode).isEqualTo("supervisor_code_1")
      assertThat(assignment.assignedByUsername).isEqualTo("ADMIN_USER1")
    }

    @Test
    fun `assigning another supervisor preserves both assignments`() {
      sessionSupervisorEntityRepository.save(
        SessionSupervisorEntity(
          projectCode = "123",
          day = LocalDate.of(2025, 1, 9),
          supervisorCode = "OTHER_USER",
          assignedByUsername = "OTHER_ASSIGNER",
        ),
      )

      webTestClient.post()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .bodyValue(AssignSupervisorDto("supervisor_code_1"))
        .exchange()
        .expectStatus()
        .isOk

      assertThat(sessionSupervisorEntityRepository.findAll().map { it.supervisorCode })
        .containsExactlyInAnyOrder("OTHER_USER", "supervisor_code_1")
    }
  }

  @Nested
  @DisplayName("DELETE /admin/projects/123/sessions/2025-01-09/supervisor/{supervisorCode}")
  inner class UnassignSupervisorEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor/super1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor/super1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor/super1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `delete existing assignment`() {
      sessionSupervisorEntityRepository.deleteAll()

      sessionSupervisorEntityRepository.save(
        SessionSupervisorEntity(
          projectCode = "123",
          day = LocalDate.of(2025, 1, 9),
          supervisorCode = "super1",
          assignedByUsername = "user1",
        ),
      )

      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor/super1")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .exchange()
        .expectStatus()
        .isOk

      assertThat(sessionSupervisorEntityRepository.count()).isEqualTo(0)
    }

    @Test
    fun `delete succeeds if no assignment exists`() {
      sessionSupervisorEntityRepository.deleteAll()

      webTestClient.delete()
        .uri("/admin/projects/123/sessions/2025-01-09/supervisor/super1")
        .addAdminUiAuthHeader(username = "ADMIN_USER1")
        .exchange()
        .expectStatus()
        .isOk
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AllocateSupervisorToSessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
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
  @DisplayName("GET /admin/projects/session-search")
  inner class SessionSearchEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/admin/projects/session-search")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session summaries`() {
      CommunityPaybackAndDeliusMockServer.getSessions(
        providerCode = "UNKNOWN",
        teamCode = "999",
        startDate = LocalDate.of(2025, 1, 9),
        endDate = LocalDate.of(2025, 7, 9),
        projectSessions = SessionSummaries(
          listOf(
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Community Garden Maintenance")),
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Park Cleanup")),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/admin/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
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
        providerCode = "UNKNOWN",
        teamCode = "999",
        startDate = LocalDate.of(2025, 1, 9),
        endDate = LocalDate.of(2025, 7, 9),
        projectSessions = SessionSummaries(emptyList()),
      )

      val sessionSummaries = webTestClient.get()
        .uri("/admin/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(sessionSummaries.allocations).isEmpty()
    }
  }

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
        session = Session.valid().copy(
          project = Project.valid().copy(
            name = "Community Garden Maintenance",
            code = "N123456789",
          ),
          appointmentSummaries = listOf(
            AppointmentSummary.valid().copy(outcome = null),
            AppointmentSummary.valid().copy(outcome = null),
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

    @Test
    fun `Correctly handles limited offenders`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        username = "USER1",
        date = LocalDate.of(2025, 1, 9),
        session = Session.valid().copy(
          project = Project.valid().copy(
            code = "N123456789",
          ),
          appointmentSummaries = listOf(
            AppointmentSummary.valid().copy(
              id = 1L,
              case = CaseSummary.valid().copy(
                crn = "CRN1",
                currentExclusion = true,
              ),
              outcome = null,
            ),
            AppointmentSummary.valid().copy(
              id = 2L,
              case = CaseSummary.valid().copy(
                crn = "CRN2",
                currentExclusion = true,
              ),
              outcome = null,
            ),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.usersAccess(
        username = "USER1",
        crns = listOf("CRN1", "CRN2"),
        response = UserAccess(
          listOf(
            CaseAccess(crn = "CRN1", userExcluded = false, userRestricted = false),
            CaseAccess(crn = "CRN2", userExcluded = true, userRestricted = false),
          ),
        ),
      )

      val session = webTestClient.get()
        .uri("/admin/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addAdminUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(session.appointmentSummaries).hasSize(2)

      assertThat(session.appointmentSummaries[0].offender.crn).isEqualTo("CRN1")
      assertThat(session.appointmentSummaries[0].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)

      assertThat(session.appointmentSummaries[1].offender.crn).isEqualTo("CRN2")
      assertThat(session.appointmentSummaries[1].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate
import java.time.LocalTime

class AdminSessionsIT : IntegrationTestBase() {

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
        SessionSummaries(
          listOf(
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(name = "Community Garden Maintenance")),
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(name = "Park Cleanup")),
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
        SessionSummaries(emptyList()),
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
  inner class ProjectSessionsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09?start=09:00&end=17:00")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/admin/projects/123/sessions/2025-01-09")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        Session.valid().copy(
          project = Project.valid().copy(
            name = "Community Garden Maintenance",
            code = "N123456789",
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            AppointmentSummary.valid().copy(outcome = null),
            AppointmentSummary.valid().copy(outcome = null),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/admin/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(sessionSearchResults.projectName).isEqualTo("Community Garden Maintenance")
      assertThat(sessionSearchResults.projectCode).isEqualTo("N123456789")
      assertThat(sessionSearchResults.endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(sessionSearchResults.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(sessionSearchResults.date).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(sessionSearchResults.appointmentSummaries).hasSize(2)
    }

    @Test
    fun `Correctly handles limited offenders`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        Session.valid().copy(
          project = Project.valid().copy(
            code = "N123456789",
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
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
}

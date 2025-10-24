package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate
import java.time.LocalTime

class SupervisorProjectsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /supervisor/projects/123/sessions/2025-01-09")
  inner class ProjectSessionsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09?start=09:00&end=17:00")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        ProjectSession.valid().copy(
          project = Project.valid().copy(
            name = "Community Garden Maintenance",
            code = "N123456789",
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary.valid().copy(outcome = null),
            ProjectAppointmentSummary.valid().copy(outcome = null),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/supervisor/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addSupervisorUiAuthHeader()
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
        ProjectSession.valid().copy(
          project = Project.valid().copy(
            code = "N123456789",
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary.valid().copy(
              id = 1L,
              case = CaseSummary.valid().copy(
                crn = "CRN1",
                currentExclusion = true,
              ),
              outcome = null,
            ),
            ProjectAppointmentSummary.valid().copy(
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
        .uri("/supervisor/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addSupervisorUiAuthHeader(username = "USER1")
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

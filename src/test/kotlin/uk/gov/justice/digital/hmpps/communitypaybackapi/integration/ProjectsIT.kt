package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionSummariesDto
import java.time.LocalDate
import java.time.LocalTime

class ProjectsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /projects/session-search")
  inner class ProjectAllocationsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/session-search?startDate=2025-09-01&endDate=2025-09-07&teamCode=1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/projects/session-search")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session summaries`() {
      CommunityPaybackAndDeliusMockServer.getSessions(
        ProjectSessionSummaries(
          listOf(
            ProjectSessionSummary.valid().copy(project = ProjectSummary.valid().copy(name = "Community Garden Maintenance")),
            ProjectSessionSummary.valid().copy(project = ProjectSummary.valid().copy(name = "Park Cleanup")),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .addUiAuthHeader()
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
        ProjectSessionSummaries(emptyList()),
      )

      val sessionSummaries = webTestClient.get()
        .uri("/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(sessionSummaries.allocations).isEmpty()
    }
  }

  @Nested
  @DisplayName("GET /projects/123/sessions/2025-01-09")
  inner class ProjectSessionsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09?start=09:00&end=17:00")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project session`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        ProjectSession(
          project = Project(
            name = "Community Garden Maintenance",
            code = "N123456789",
            location = ProjectLocation(
              streetName = "Somehere Lane",
              county = "Surrey",
            ),
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary(
              id = 1L,
              case = CaseSummary.valid().copy(crn = "CRN1"),
              requirementProgress = RequirementProgress(
                requirementMinutes = 520,
                completedMinutes = 30,
              ),
            ),
            ProjectAppointmentSummary(
              id = 2L,
              case = CaseSummary.valid().copy(crn = "CRN2"),
              requirementProgress = RequirementProgress(
                requirementMinutes = 600,
                completedMinutes = 60,
              ),
            ),
          ),
        ),
      )

      val sessionSearchResults = webTestClient.get()
        .uri("/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addUiAuthHeader()
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
      assertThat(sessionSearchResults.appointmentSummaries[0].id).isEqualTo(1L)
      assertThat(sessionSearchResults.appointmentSummaries[0].requirementMinutes).isEqualTo(520)
      assertThat(sessionSearchResults.appointmentSummaries[0].completedMinutes).isEqualTo(30)
      assertThat(sessionSearchResults.appointmentSummaries[0].offender.crn).isEqualTo("CRN1")
      assertThat(sessionSearchResults.appointmentSummaries[0].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }

    @Test
    fun `Correctly handles limited offenders`() {
      CommunityPaybackAndDeliusMockServer.getProjectSession(
        ProjectSession(
          project = Project(
            name = "Community Garden Maintenance",
            code = "N123456789",
            location = ProjectLocation(
              streetName = "Somehere Lane",
              county = "Surrey",
            ),
          ),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary(
              id = 1L,
              case = CaseSummary.valid().copy(
                crn = "CRN1",
                currentExclusion = true,
              ),
              requirementProgress = RequirementProgress(
                requirementMinutes = 520,
                completedMinutes = 30,
              ),
            ),
            ProjectAppointmentSummary(
              id = 2L,
              case = CaseSummary.valid().copy(
                crn = "CRN2",
                currentExclusion = true,
              ),
              requirementProgress = RequirementProgress(
                requirementMinutes = 600,
                completedMinutes = 60,
              ),
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
        .uri("/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addUiAuthHeader(username = "USER1")
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

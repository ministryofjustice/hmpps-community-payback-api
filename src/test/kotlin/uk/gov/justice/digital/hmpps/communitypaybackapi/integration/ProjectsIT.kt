package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
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
    fun `should return OK with project allocations`() {
      CommunityPaybackAndDeliusMockServer.projectSessionSummaries(
        ProjectSessionSummaries(
          listOf(
            ProjectSummary(
              id = 1L,
              projectId = 101L,
              projectName = "Community Garden Maintenance",
              date = LocalDate.of(2025, 9, 1),
              startTime = LocalTime.of(9, 0),
              endTime = LocalTime.of(17, 0),
              projectCode = "cgm",
              numberOfOffendersAllocated = 0,
              numberOfOffendersWithOutcomes = 1,
              numberOfOffendersWithEA = 2,
            ),
            ProjectSummary(
              id = 2L,
              projectId = 201L,
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
        .uri("/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

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
      CommunityPaybackAndDeliusMockServer.projectSessionSummaries(
        ProjectSessionSummaries(emptyList()),
      )

      val allocations = webTestClient.get()
        .uri("/projects/session-search?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(allocations.allocations).isEmpty()
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
      CommunityPaybackAndDeliusMockServer.projectSessions(
        ProjectSession(
          projectName = "Community Garden Maintenance",
          projectCode = "N123456789",
          projectLocation = "Somwhere Lane, Surrey",
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary(
              id = 1L,
              crn = "CRN1",
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
            ProjectAppointmentSummary(
              id = 2L,
              crn = "CRN2",
              requirementMinutes = 600,
              completedMinutes = 60,
            ),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.probationCasesSummaries(
        crns = listOf("CRN1", "CRN2"),
        response = CaseSummaries(
          listOf(
            CaseSummary(crn = "CRN1", name = CaseName("Jeff", "Jeffity")),
            CaseSummary(crn = "CRN2", name = CaseName("Jim", "Jimmity")),
          ),
        ),
      )

      val session = webTestClient.get()
        .uri("/projects/N123456789/sessions/2025-01-09?startTime=09:00&endTime=17:00")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(session.projectName).isEqualTo("Community Garden Maintenance")
      assertThat(session.projectCode).isEqualTo("N123456789")
      assertThat(session.endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(session.startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(session.date).isEqualTo(LocalDate.of(2025, 1, 9))
      assertThat(session.appointmentSummaries).hasSize(2)
      assertThat(session.appointmentSummaries[0].id).isEqualTo(1L)
      assertThat(session.appointmentSummaries[0].requirementMinutes).isEqualTo(520)
      assertThat(session.appointmentSummaries[0].completedMinutes).isEqualTo(30)
      assertThat(session.appointmentSummaries[0].offender.crn).isEqualTo("CRN1")
      assertThat(session.appointmentSummaries[0].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }

    @Test
    fun `Correctly handles limited and not found offenders`() {
      CommunityPaybackAndDeliusMockServer.projectSessions(
        ProjectSession(
          projectName = "Community Garden Maintenance",
          projectCode = "N123456789",
          projectLocation = "Somwhere Lane, Surrey",
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(17, 0),
          date = LocalDate.of(2025, 1, 9),
          appointmentSummaries = listOf(
            ProjectAppointmentSummary(
              id = 1L,
              crn = "CRN1",
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
            ProjectAppointmentSummary(
              id = 2L,
              crn = "CRN2",
              requirementMinutes = 600,
              completedMinutes = 60,
            ),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.probationCasesSummaries(
        crns = listOf("CRN1", "CRN2"),
        response = CaseSummaries(
          listOf(
            CaseSummary(crn = "CRN2", name = CaseName("Jim", "Jimmity"), currentExclusion = true),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.usersAccess(
        username = "USER1",
        crns = listOf("CRN2"),
        response = UserAccess(
          listOf(
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
      assertThat(session.appointmentSummaries[0].offender).isInstanceOf(OffenderDto.OffenderNotFoundDto::class.java)

      assertThat(session.appointmentSummaries[1].offender.crn).isEqualTo("CRN2")
      assertThat(session.appointmentSummaries[1].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
    }
  }
}

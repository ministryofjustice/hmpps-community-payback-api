package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate

class SupervisorSessionsIT : IntegrationTestBase() {

  @Autowired
  lateinit var sessionSupervisorEntityRepository: SessionSupervisorEntityRepository

  @Nested
  @DisplayName("GET /supervisor/projects/{projectCode}/sessions/{date}")
  inner class GetSessionEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/projects/123/sessions/2025-01-09")
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
        username = "USER1",
        date = LocalDate.of(2025, 1, 9),
        session =
        Session.valid().copy(
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
        .uri("/supervisor/projects/N123456789/sessions/2025-01-09")
        .addSupervisorUiAuthHeader(username = "USER1")
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
        session =
        Session.valid().copy(
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

  @Nested
  @DisplayName("GET /supervisor/supervisors/{supervisorCode}/sessions/next")
  inner class GetNextSessionEndpoint {

    @BeforeEach
    fun setUp() {
      sessionSupervisorEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/supervisors/SUPERVISOR001/sessions/next")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/supervisors/SUPERVISOR001/sessions/next")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/supervisors/SUPERVISOR001/sessions/next")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 if there is no future session`() {
      val yesterday = LocalDate.now().minusDays(1)
      allocateSessionToSupervisor1("IGNORED_PROJECT_YESTERDAY", yesterday)

      webTestClient.get()
        .uri("/supervisor/supervisors/SUPERVISOR001/sessions/next")
        .addSupervisorUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return OK with next project session`() {
      val yesterday = LocalDate.now().minusDays(1)
      allocateSessionToSupervisor1("IGNORED_PROJECT_YESTERDAY", yesterday)

      val nextYear = LocalDate.now().plusYears(5)
      allocateSessionToSupervisor1("PROJ2", nextYear)

      val today = LocalDate.now()
      allocateSessionToSupervisor1("PROJ1", today)

      CommunityPaybackAndDeliusMockServer.getProjectSession(
        username = "USER1",
        date = today,
        session = Session.valid(ctx).copy(
          project = Project.valid().copy(code = "PROJ1"),
        ),
      )

      val result = webTestClient.get()
        .uri("/supervisor/supervisors/SUPERVISOR001/sessions/next")
        .addSupervisorUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummaryDto>()

      assertThat(result.projectCode).isEqualTo("PROJ1")
    }
  }

  @Nested
  @DisplayName("GET /supervisor/supervisors/{supervisorCode}/sessions/future")
  inner class GetFutureSessionEndpoint {

    @BeforeEach
    fun setUp() {
      sessionSupervisorEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/providers/P123/teams/T456/sessions/future")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/providers/P123/teams/T456/sessions/future")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/providers/P123/teams/T456/sessions/future")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with project session`() {
      CommunityPaybackAndDeliusMockServer.getSessions(
        providerCode = "P123",
        teamCode = "T456",
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(7),
        projectSessions = SessionSummaries(
          listOf(
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Community Garden Maintenance")),
            SessionSummary.valid().copy(project = ProjectSummary.valid().copy(description = "Park Cleanup")),
          ),
        ),
      )

      val result = webTestClient.get()
        .uri("/supervisor/providers/P123/teams/T456/sessions/future")
        .addSupervisorUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(result.allocations).hasSize(2)
      assertThat(result.allocations[0].projectName).isEqualTo("Community Garden Maintenance")
      assertThat(result.allocations[1].projectName).isEqualTo("Park Cleanup")
    }
  }

  private fun allocateSessionToSupervisor1(
    projectCode: String,
    day: LocalDate,
  ) {
    sessionSupervisorEntityRepository.save(
      SessionSupervisorEntity.valid().copy(
        projectCode = projectCode,
        day = day,
        supervisorCode = "SUPERVISOR001",
      ),
    )
  }
}

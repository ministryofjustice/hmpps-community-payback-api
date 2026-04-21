package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummariesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SessionSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
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
      CommunityPaybackAndDeliusMockServer.setupGetProjectResponse(
        NDProject.valid(ctx).copy(
          name = "Community Garden Maintenance",
          code = "N123456789",
        ),
      )

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "USER1",
        pageSize = Int.MAX_VALUE,
        fromDate = LocalDate.of(2025, 1, 9),
        toDate = LocalDate.of(2025, 1, 9),
        projectCodes = listOf("N123456789"),
        appointments = listOf(
          NDAppointmentSummary.valid().copy(outcome = null),
          NDAppointmentSummary.valid().copy(outcome = null),
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

      CommunityPaybackAndDeliusMockServer.setupGetProjectResponse(NDProject.valid(ctx).copy(code = "PROJ1"))

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "USER1",
        pageSize = Int.MAX_VALUE,
        fromDate = today,
        toDate = today,
        projectCodes = listOf("PROJ1"),
        appointments = listOf(
          NDAppointmentSummary.valid(ctx).copy(),
          NDAppointmentSummary.valid(ctx).copy(),
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
      val projectName1 = "Community Garden Maintenance"
      val projectName2 = "Park Cleanup"

      val sessions = listOf(
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName1)),
        NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName2)),
      )

      CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
        providerCode = "P123",
        teamCode = "T456",
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(7),
        typeCode = listOf("NP1", "NP2", "PL"),
        projectSessions = NDSessionSummaries(
          sessions,
          pageResponse = PageResponse(
            content = sessions,
            page = PageResponse.PageMeta(50, 0, 2, 1),
          ),
        ),
        sortString = "date,desc",
      )

      val result = webTestClient.get()
        .uri("/supervisor/providers/P123/teams/T456/sessions/future")
        .addSupervisorUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionSummariesDto>()

      assertThat(result.allocations).hasSize(2)
      assertThat(result.allocations[0].projectName).isEqualTo(projectName1)
      assertThat(result.allocations[1].projectName).isEqualTo(projectName2)
      assertThat(result.content).hasSize(2)
      assertThat(result.content[0].projectName).isEqualTo(projectName1)
      assertThat(result.content[1].projectName).isEqualTo(projectName2)
      assertThat(result.page.size).isEqualTo(50)
      assertThat(result.page.totalPages).isEqualTo(1)
      assertThat(result.page.totalElements).isEqualTo(2)
      assertThat(result.page.number).isEqualTo(0)
    }
  }

  @Test
  fun `should return OK with paginated response when pagination parameters are provided`() {
    val projectName1 = "Community Garden Maintenance"
    val projectName2 = "Park Cleanup"

    val sessions = listOf(
      NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName1)),
      NDSessionSummary.valid().copy(project = NDProjectSummary.valid().copy(description = projectName2)),
    )

    CommunityPaybackAndDeliusMockServer.setupGetSessionsResponse(
      providerCode = "P123",
      teamCode = "T456",
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(7),
      typeCode = listOf("NP1", "NP2", "PL"),
      projectSessions = NDSessionSummaries(
        sessions,
        pageResponse = PageResponse(
          content = sessions,
          page = PageResponse.PageMeta(25, 0, 2, 1),
        ),
      ),
      sortString = "projectName,asc",
    )

    val result = webTestClient.get()
      .uri("/supervisor/providers/P123/teams/T456/sessions/future?page=0&size=25&sort=projectName,asc")
      .addSupervisorUiAuthHeader(username = "USER1")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject<SessionSummariesDto>()

    assertThat(result.allocations).hasSize(2)
    assertThat(result.allocations[0].projectName).isEqualTo(projectName1)
    assertThat(result.allocations[1].projectName).isEqualTo(projectName2)
    assertThat(result.content).hasSize(2)
    assertThat(result.content[0].projectName).isEqualTo(projectName1)
    assertThat(result.content[1].projectName).isEqualTo(projectName2)
    assertThat(result.page.size).isEqualTo(25)
    assertThat(result.page.totalPages).isEqualTo(1)
    assertThat(result.page.totalElements).isEqualTo(2)
    assertThat(result.page.number).isEqualTo(0)
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

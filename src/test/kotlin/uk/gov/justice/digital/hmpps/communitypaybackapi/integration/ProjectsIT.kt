package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.ProjectAllocationsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto
import java.time.LocalDate
import java.time.LocalTime

class ProjectsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /projects/allocations")
  inner class ProjectAllocationsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/allocations?startDate=2025-09-01&endDate=2025-09-07&teamId=1")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/projects/allocations")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project allocations`() {
      CommunityPaybackAndDeliusMockServer.projectAllocations(
        ProjectAllocations(
          listOf(
            ProjectAllocation(
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
            ProjectAllocation(
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
        .uri("/projects/allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAllocationsDto>()

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
      CommunityPaybackAndDeliusMockServer.projectAllocations(
        ProjectAllocations(emptyList()),
      )

      val allocations = webTestClient.get()
        .uri("/projects/allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAllocationsDto>()

      assertThat(allocations.allocations).isEmpty()
    }
  }

  @Nested
  @DisplayName("GET /projects/123/sessions/2025-01-09/appointments")
  inner class ProjectAppointmentsEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments?start=09:00&end=17:00")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments?start=09:00&end=17:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments?start=09:00&end=17:00")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request if missing parameters`() {
      webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .is4xxClientError
    }

    @Test
    fun `should return OK with project appointments`() {
      CommunityPaybackAndDeliusMockServer.projectAppointments(
        projectId = 123L,
        date = LocalDate.of(2025, 1, 9),
        start = LocalTime.of(9, 0),
        end = LocalTime.of(17, 0),
        ProjectAppointments(
          listOf(
            ProjectAppointment(
              id = 1L,
              projectName = "Community Garden Maintenance",
              projectCode = "N123456789",
              crn = "CRN1",
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
            ProjectAppointment(
              id = 2L,
              projectName = "Park Cleanup",
              projectCode = "N987654321",
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

      val allocations = webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments?start=09:00&end=17:00")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(allocations.appointments).hasSize(2)
      assertThat(allocations.appointments[0].id).isEqualTo(1L)
      assertThat(allocations.appointments[0].projectName).isEqualTo("Community Garden Maintenance")
      assertThat(allocations.appointments[0].requirementMinutes).isEqualTo(520)
      assertThat(allocations.appointments[0].completedMinutes).isEqualTo(30)
      assertThat(allocations.appointments[0].offender.crn).isEqualTo("CRN1")
      assertThat(allocations.appointments[0].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }

    @Test
    fun `Correctly handles limited and not found offenders`() {
      CommunityPaybackAndDeliusMockServer.projectAppointments(
        projectId = 123L,
        date = LocalDate.of(2025, 1, 9),
        start = LocalTime.of(9, 0),
        end = LocalTime.of(17, 0),
        ProjectAppointments(
          listOf(
            ProjectAppointment(
              id = 1L,
              projectName = "Community Garden Maintenance",
              projectCode = "N123456789",
              crn = "CRN1",
              requirementMinutes = 520,
              completedMinutes = 30,
            ),
            ProjectAppointment(
              id = 2L,
              projectName = "Park Cleanup",
              projectCode = "N987654321",
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

      val allocations = webTestClient.get()
        .uri("/projects/123/sessions/2025-01-09/appointments?start=09:00&end=17:00")
        .addUiAuthHeader(username = "USER1")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<SessionDto>()

      assertThat(allocations.appointments).hasSize(2)

      assertThat(allocations.appointments[0].offender.crn).isEqualTo("CRN1")
      assertThat(allocations.appointments[0].offender).isInstanceOf(OffenderDto.OffenderNotFoundDto::class.java)

      assertThat(allocations.appointments[1].offender.crn).isEqualTo("CRN2")
      assertThat(allocations.appointments[1].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
    }
  }
}

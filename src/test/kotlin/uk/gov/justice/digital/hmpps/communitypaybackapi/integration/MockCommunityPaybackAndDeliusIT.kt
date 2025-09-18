package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.EnforcementActions
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusController.MockCommunityPaybackAndDeliusRepository
import java.time.LocalDate
import java.time.LocalTime

class MockCommunityPaybackAndDeliusIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /mocks/community-payback-and-delius/providers")
  inner class GetProviders {
    @Test
    fun `returns fixed list of providers`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/providers")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderSummaries>()

      assertThat(response.providers).hasSize(3)

      assertThat(response.providers[0].id).isEqualTo(1000)
      assertThat(response.providers[0].name).isEqualTo("East of England")
      assertThat(response.providers[1].id).isEqualTo(2000)
      assertThat(response.providers[1].name).isEqualTo("North East Region")
      assertThat(response.providers[2].id).isEqualTo(3000)
      assertThat(response.providers[2].name).isEqualTo("North West Region")
    }
  }

  @Nested
  @DisplayName("GET /mocks/community-payback-and-delius/provider-teams")
  inner class GetProviderTeams {
    @Test
    fun `returns fixed list of teams`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/provider-teams?providerId=1000")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProviderTeamSummaries>()

      assertThat(response.teams).hasSize(3)

      assertThat(response.teams[0].id).isEqualTo(1001)
      assertThat(response.teams[0].name).isEqualTo("Team Lincoln")
      assertThat(response.teams[1].id).isEqualTo(2001)
      assertThat(response.teams[1].name).isEqualTo("Team Grantham")
      assertThat(response.teams[2].id).isEqualTo(3001)
      assertThat(response.teams[2].name).isEqualTo("Team Boston")
    }
  }

  @Nested
  @DisplayName("GET /mocks/community-payback-and-delius/project-allocations")
  inner class GetProjectAllocations {
    @Test
    fun `returns fixed list of teams`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/project-allocations?teamId=52")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAllocations>()

      assertThat(response.allocations).hasSize(2)

      assertThat(response.allocations[0].id).isEqualTo(1L)
      assertThat(response.allocations[0].projectName).isEqualTo("Community Garden")
      assertThat(response.allocations[0].date).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(response.allocations[0].startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(response.allocations[0].endTime).isEqualTo(LocalTime.of(17, 0))
      assertThat(response.allocations[0].projectCode).isEqualTo("cg")
      assertThat(response.allocations[0].numberOfOffendersAllocated).isEqualTo(2)
      assertThat(response.allocations[0].numberOfOffendersWithOutcomes).isEqualTo(0)
      assertThat(response.allocations[0].numberOfOffendersWithEA).isEqualTo(0)

      assertThat(response.allocations[1].id).isEqualTo(2L)
      assertThat(response.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(response.allocations[1].date).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(response.allocations[1].startTime).isEqualTo(LocalTime.of(8, 0))
      assertThat(response.allocations[1].endTime).isEqualTo(LocalTime.of(16, 0))
      assertThat(response.allocations[1].projectCode).isEqualTo("pc")
      assertThat(response.allocations[1].numberOfOffendersAllocated).isEqualTo(1)
      assertThat(response.allocations[1].numberOfOffendersWithOutcomes).isEqualTo(0)
      assertThat(response.allocations[1].numberOfOffendersWithEA).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /mocks/community-payback-and-delius/projects/{projectId}/appointments")
  inner class GetProjectAppointments {

    @Test
    fun `no corresponding allocation, return empty results`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/projects/${MockCommunityPaybackAndDeliusRepository.PROJECT1_ID}/appointments?date=2024-09-01")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAppointments>()

      assertThat(response.appointments).isEmpty()
    }

    @Test
    fun `has corresponding allocation with appointments`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/projects/${MockCommunityPaybackAndDeliusRepository.PROJECT1_ID}/appointments?date=2025-09-01")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<ProjectAppointments>()

      assertThat(response.appointments).hasSize(2)

      assertThat(response.appointments[0].id).isEqualTo(1L)
      assertThat(response.appointments[0].crn).isEqualTo(MockCommunityPaybackAndDeliusRepository.CRN1)
      assertThat(response.appointments[0].projectName).isEqualTo("Community Garden")
      assertThat(response.appointments[0].requirementMinutes).isEqualTo(600)
      assertThat(response.appointments[0].completedMinutes).isEqualTo(60)

      assertThat(response.appointments[1].id).isEqualTo(2L)
      assertThat(response.appointments[1].crn).isEqualTo(MockCommunityPaybackAndDeliusRepository.CRN2)
      assertThat(response.appointments[1].projectName).isEqualTo("Community Garden")
      assertThat(response.appointments[1].requirementMinutes).isEqualTo(300)
      assertThat(response.appointments[1].completedMinutes).isEqualTo(30)
    }
  }

  @Nested
  @DisplayName("POST /mocks/community-payback-and-delius/references/enforcement-actions")
  inner class GetEnforcementActions {

    @Test
    fun `returns fixed list of enforcement actions`() {
      val response = webTestClient.get()
        .uri("/mocks/community-payback-and-delius/references/enforcement-actions")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<EnforcementActions>()

      assertThat(response.enforcementActions).hasSize(19)

      assertThat(response.enforcementActions[0].id).isEqualTo(1L)
      assertThat(response.enforcementActions[0].name).isEqualTo("Breach / Recall Initiated")
    }
  }

  @Nested
  @DisplayName("POST /mocks/community-payback-and-delius/probation-cases/summaries")
  inner class ProbationCasesSummaries {

    @Test
    fun `returns information for matching CRNs`() {
      val response = webTestClient.post()
        .uri("/mocks/community-payback-and-delius/probation-cases/summaries")
        .bodyValue(listOf("CRN0001", "CRN0003", "CRNWRONG"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CaseSummaries>()

      assertThat(response.cases).hasSize(2)
      assertThat(response.cases.map { it.crn }).containsExactlyInAnyOrder("CRN0001", "CRN0003")
    }
  }

  @Nested
  @DisplayName("POST /mocks/community-payback-and-delius/users/access")
  inner class UsersAccess {

    @Test
    fun `return CRN0003 restricted if username ends 's'`() {
      val response = webTestClient.post()
        .uri("/mocks/community-payback-and-delius/users/access?username=abcdefs")
        .bodyValue(listOf("CRN0003"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<UserAccess>()

      assertThat(response.access).hasSize(1)
      assertThat(response.access[0].crn).isEqualTo("CRN0003")
      assertThat(response.access[0].userRestricted).isEqualTo(true)
      assertThat(response.access[0].userExcluded).isEqualTo(false)
    }

    @Test
    fun `return CRN0003 as not restricted if username doesn't end 's'`() {
      val response = webTestClient.post()
        .uri("/mocks/community-payback-and-delius/users/access?username=abcdefg")
        .bodyValue(listOf("CRN0003"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<UserAccess>()

      assertThat(response.access).hasSize(1)
      assertThat(response.access[0].crn).isEqualTo("CRN0003")
      assertThat(response.access[0].userRestricted).isEqualTo(false)
      assertThat(response.access[0].userExcluded).isEqualTo(false)
    }

    @Test
    fun `return result for all CRNs, even if they don't have access records`() {
      val response = webTestClient.post()
        .uri("/mocks/community-payback-and-delius/users/access?username=abcdefg")
        .bodyValue(listOf("CRN0001", "CRNWRONG"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<UserAccess>()

      assertThat(response.access).hasSize(2)

      assertThat(response.access[0].crn).isEqualTo("CRN0001")
      assertThat(response.access[0].userRestricted).isEqualTo(false)
      assertThat(response.access[0].userExcluded).isEqualTo(false)

      assertThat(response.access[1].crn).isEqualTo("CRNWRONG")
      assertThat(response.access[1].userRestricted).isEqualTo(false)
      assertThat(response.access[1].userExcluded).isEqualTo(false)
    }
  }
}

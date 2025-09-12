package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import java.time.LocalDate

class MockCommunityPaybackAndDeliusIT : IntegrationTestBase() {

  @Nested
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
      assertThat(response.allocations[0].teamId).isEqualTo(52)
      assertThat(response.allocations[0].startDate).isEqualTo(LocalDate.of(2025, 9, 1))
      assertThat(response.allocations[0].endDate).isEqualTo(LocalDate.of(2025, 9, 7))
      assertThat(response.allocations[0].projectCode).isEqualTo("cg")
      assertThat(response.allocations[0].allocated).isEqualTo(40)
      assertThat(response.allocations[0].outcomes).isEqualTo(0)
      assertThat(response.allocations[0].enforcements).isEqualTo(0)

      assertThat(response.allocations[1].id).isEqualTo(2L)
      assertThat(response.allocations[1].projectName).isEqualTo("Park Cleanup")
      assertThat(response.allocations[1].teamId).isEqualTo(52)
      assertThat(response.allocations[1].startDate).isEqualTo(LocalDate.of(2025, 9, 8))
      assertThat(response.allocations[1].endDate).isEqualTo(LocalDate.of(2025, 9, 14))
      assertThat(response.allocations[1].projectCode).isEqualTo("pc")
      assertThat(response.allocations[1].allocated).isEqualTo(3)
      assertThat(response.allocations[1].outcomes).isEqualTo(4)
      assertThat(response.allocations[1].enforcements).isEqualTo(5)
    }
  }
}

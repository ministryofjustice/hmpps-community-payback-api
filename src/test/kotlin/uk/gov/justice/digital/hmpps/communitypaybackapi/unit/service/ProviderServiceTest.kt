package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProviderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.TeamId
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@ExtendWith(MockKExtension::class)
class ProviderServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @InjectMockKs
  private lateinit var service: ProviderService

  companion object {
    const val PROVIDER_CODE = "PROV1"
    const val TEAM_CODE = "TEAM1"
  }

  @Nested
  inner class GetTeamUnallocatedSupervisor {

    @Test
    fun `returns unallocated supervisor`() {
      val supervisor1 = NDSupervisorSummary.valid().copy(code = "1", unallocated = false)
      val supervisor2 = NDSupervisorSummary.valid().copy(code = "2", unallocated = true)
      val supervisor3 = NDSupervisorSummary.valid().copy(code = "3", unallocated = false)

      every {
        communityPaybackAndDeliusClient.getTeamSupervisors(PROVIDER_CODE, TEAM_CODE)
      } returns NDSupervisorSummaries(listOf(supervisor1, supervisor2, supervisor3))

      assertThat(
        service.getTeamUnallocatedSupervisor(TeamId(PROVIDER_CODE, TEAM_CODE)),
      ).isEqualTo(supervisor2.toDto())
    }

    @Test
    fun `error if no unallocated supervisor`() {
      val supervisor1 = NDSupervisorSummary.valid().copy(code = "1", unallocated = false)
      val supervisor2 = NDSupervisorSummary.valid().copy(code = "2", unallocated = false)
      val supervisor3 = NDSupervisorSummary.valid().copy(code = "3", unallocated = false)

      every {
        communityPaybackAndDeliusClient.getTeamSupervisors(PROVIDER_CODE, TEAM_CODE)
      } returns NDSupervisorSummaries(listOf(supervisor1, supervisor2, supervisor3))

      assertThatThrownBy {
        service.getTeamUnallocatedSupervisor(TeamId(PROVIDER_CODE, TEAM_CODE))
      }.hasMessage("Can't find unallocated supervisor for team 'TeamId(providerCode=PROV1, teamCode=TEAM1)'")
    }
  }
}

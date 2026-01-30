package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorTeamDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class SupervisorMappersTest {

  @Nested
  inner class SupervisorToSupervisorDto {

    @Test
    fun success() {
      val result = NDSupervisor(
        code = "SC1",
        isUnpaidWorkTeamMember = true,
        unpaidWorkTeams = listOf(
          NDSupervisorTeam(
            code = "ST1",
            description = "Team 1",
            provider = NDCodeDescription("provider1", "description1"),
          ),
          NDSupervisorTeam(
            code = "ST2",
            description = "Team 2",
            provider = NDCodeDescription("provider2", "description2"),
          ),
        ),
      ).toDto()

      assertThat(result.code).isEqualTo("SC1")
      assertThat(result.isUnpaidWorkTeamMember).isTrue()
      assertThat(result.unpaidWorkTeams).hasSize(2)

      assertThat(result.unpaidWorkTeams).containsExactly(
        SupervisorTeamDto(
          code = "ST1",
          description = "Team 1",
          provider = ProviderSummaryDto("provider1", "description1"),
        ),
        SupervisorTeamDto(
          code = "ST2",
          description = "Team 2",
          provider = ProviderSummaryDto("provider2", "description2"),
        ),
      )
    }
  }
}

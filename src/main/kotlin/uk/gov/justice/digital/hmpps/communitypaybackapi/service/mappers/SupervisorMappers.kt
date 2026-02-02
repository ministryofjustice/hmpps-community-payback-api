package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorTeam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProviderSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorTeamDto

fun NDSupervisor.toDto() = SupervisorDto(
  code = this.code,
  isUnpaidWorkTeamMember = this.isUnpaidWorkTeamMember,
  unpaidWorkTeams = this.unpaidWorkTeams.map { it.toDto() },
)

fun NDSupervisorTeam.toDto() = SupervisorTeamDto(
  code = this.code,
  description = this.description,
  provider = this.provider.let {
    ProviderSummaryDto(
      code = it.code,
      name = it.description,
    )
  },
)

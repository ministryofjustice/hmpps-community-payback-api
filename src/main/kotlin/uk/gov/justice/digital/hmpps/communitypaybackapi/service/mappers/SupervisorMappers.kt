package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Supervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorDto

fun Supervisor.toDto() = SupervisorDto(
  code = this.code,
  isUnpaidWorkTeamMember = this.isUnpaidWorkTeamMember,
)

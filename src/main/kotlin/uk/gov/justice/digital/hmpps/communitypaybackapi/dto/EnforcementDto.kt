package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate
import java.util.UUID

data class EnforcementDto(
  val enforcementActionId: UUID,
  val respondBy: LocalDate? = null,
)

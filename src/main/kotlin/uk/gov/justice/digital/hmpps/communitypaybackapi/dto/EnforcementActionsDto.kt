package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class EnforcementActionDto(
  @param:Schema(description = "Enforcement outcome identifier", example = "e6e7b1d6-0a3b-4b8d-bc7a-9f1c3d8e5f2a")
  val id: UUID,
  @param:Schema(description = "Enforcement outcome name", example = "Breach Request Actioned")
  val name: String,
  @param:Schema(description = "Enforcement outcome code", example = "BRE02")
  val code: String,
  @param:Schema(description = "Indicates if 'respondByDateRequired' is required on this enforcement outcome", example = "false")
  val respondByDateRequired: Boolean,
)

data class EnforcementActionsDto(
  @param:Schema(description = "List of enforcement outcomes")
  val enforcementActions: List<EnforcementActionDto>,
)

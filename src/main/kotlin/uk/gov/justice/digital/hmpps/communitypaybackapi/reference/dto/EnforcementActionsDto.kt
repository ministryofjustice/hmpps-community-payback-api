package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto

import io.swagger.v3.oas.annotations.media.Schema

data class EnforcementActionDto(
  @param:Schema(description = "Enforcement outcome identifier", example = "2")
  val id: Long,
  @param:Schema(description = "Enforcement outcome name", example = "Breach / Recall Initiated")
  val name: String,
)

data class EnforcementActionsDto(
  @param:Schema(description = "List of enforcement outcomes")
  val enforcementActions: List<EnforcementActionDto>,
)

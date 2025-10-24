package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ContactOutcomeDto(
  @param:Schema(description = "Contact outcome identifier", example = "4306c7ca-b717-4995-9eea-91e41d95d44a")
  val id: UUID,
  @param:Schema(description = "Contact outcome name", example = "Attended - Complied")
  val name: String,
  @param:Schema(description = "Contact outcome code", example = "ATTC")
  val code: String,
  @param:Schema(description = "Whether this outcome requires an enforcement action to take place", example = "false")
  val enforceable: Boolean,
) {
  companion object
}

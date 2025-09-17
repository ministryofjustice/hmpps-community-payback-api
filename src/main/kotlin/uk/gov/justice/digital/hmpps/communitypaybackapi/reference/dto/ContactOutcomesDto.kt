package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ContactOutcomeDto(
  @param:Schema(description = "Contact outcome identifier", example = "1234")
  val id: Long,
  @param:Schema(description = "Contact outcome name", example = "Successful Contact")
  val name: String,
)

data class ContactOutcomesDto(
  @param:Schema(description = "List of contact outcomes")
  val contactOutcomes: List<ContactOutcomeDto>,
)

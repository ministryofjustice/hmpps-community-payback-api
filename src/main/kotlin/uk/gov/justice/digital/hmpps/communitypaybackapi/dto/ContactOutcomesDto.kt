package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ContactOutcomesDto(
  @param:Schema(description = "List of contact outcomes")
  val contactOutcomes: List<ContactOutcomeDto>,
)

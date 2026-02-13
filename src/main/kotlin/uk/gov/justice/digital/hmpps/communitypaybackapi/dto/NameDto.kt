package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class NameDto(
  val forename: String,
  val surname: String,
  val middleNames: List<String> = emptyList(),
) {
  companion object
}

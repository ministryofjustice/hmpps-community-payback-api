package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class PersonalCircumstancesDto(
  val travelTimeDetails: PersonalCircumstancesDetailsDto?,
) {
  @get:JsonProperty("isAllowedTravelTime")
  val isAllowedTravelTime: Boolean
    get() = travelTimeDetails != null

  companion object
}

data class PersonalCircumstancesDetailsDto(
  val startDate: OffsetDateTime,
  val endDate: OffsetDateTime?,
  val verified: Boolean,
  val notes: String?,
) {
  companion object
}

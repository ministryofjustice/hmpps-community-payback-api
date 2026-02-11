package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalTime

data class PickUpDataDto(
  val location: LocationDto?,
  val locationCode: String?,
  val locationDescription: String?,
  val time: LocalTime?,
) {
  companion object
}

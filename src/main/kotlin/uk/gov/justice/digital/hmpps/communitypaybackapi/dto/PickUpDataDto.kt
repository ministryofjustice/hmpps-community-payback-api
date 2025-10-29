package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalTime

data class PickUpDataDto(
  val location: LocationDto?,
  val time: LocalTime?,
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

data class PickUpDataDto(
  @Deprecated("Use pickupLocation instead")
  @param:Schema(description = "Use pickupLocation instead", deprecated = true)
  val location: LocationDto?,
  @Deprecated("Use pickupLocation.deliusCode instead")
  @param:Schema(description = "Use pickupLocation.deliusCode instead", deprecated = true)
  val locationCode: String?,
  @Deprecated("Use pickupLocation.description instead")
  @param:Schema(description = "Use pickupLocation.description instead", deprecated = true)
  val locationDescription: String?,
  val pickupLocation: PickUpLocationDto?,
  val time: LocalTime?,
) {
  companion object
}

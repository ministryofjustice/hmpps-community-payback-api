package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin/incentive-scheme",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminIncentiveSchemeController(
  private val incentiveSchemeService: IncentiveSchemeService,
) {
  @GetMapping(
    path = ["/details/{crn}/{deliusEventNumber}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with incentive scheme details",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender and/or incentive scheme details not found for the given CRN and Event Number",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIncentiveSchemeDetails(
    @PathVariable crn: String,
    @PathVariable deliusEventNumber: Int,
  ) = incentiveSchemeService.getDetails(crn, deliusEventNumber)

  @GetMapping(
    path = ["/metadata"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with incentive scheme metadata",
      ),
    ],
  )
  fun getIncentiveSchemeMetadata() = incentiveSchemeService.getMetadata()
}

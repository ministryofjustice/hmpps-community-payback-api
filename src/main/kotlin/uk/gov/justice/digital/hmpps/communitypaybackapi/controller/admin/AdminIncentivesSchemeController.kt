package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ErrorResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeService

@AdminUiController
@RequestMapping(
  "/admin/incentives-scheme",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminIncentivesSchemeController(
  private val incentivesSchemeService: IncentivesSchemeService,
) {
  @GetMapping(
    path = ["/details/{crn}/{deliusEventNumber}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with incentives scheme details",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender and/or incentives scheme details not found for the given CRN and Event Number",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIncentivesSchemeDetails(
    @PathVariable crn: String,
    @PathVariable deliusEventNumber: Int,
  ) = incentivesSchemeService.getDetails(crn, deliusEventNumber)

  @GetMapping(
    path = ["/metadata"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with incentives scheme metadata",
      ),
    ],
  )
  fun getIncentivesSchemeMetadata() = incentivesSchemeService.getMetadata()
}

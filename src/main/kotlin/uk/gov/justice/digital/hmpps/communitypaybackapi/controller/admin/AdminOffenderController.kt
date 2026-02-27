package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminOffenderController(private val offenderService: OffenderService) {

  @GetMapping(
    path = ["/offenders/{crn}/unpaid-work-details/{deliusEventNumber}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get offender summary by CRN",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with offender summary",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender and/or Unpaid Work Details not found for the given CRN and Event Number",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getEvent(@PathVariable crn: String, @PathVariable deliusEventNumber: Long): UnpaidWorkDetailsDto = offenderService.getUnpaidWorkDetails(crn, deliusEventNumber)

  @GetMapping(
    path = ["/offenders/{crn}/summary"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get offender summary by CRN",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with offender summary",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found for the given CRN",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffenderSummary(@PathVariable crn: String): CaseDetailsSummaryDto = offenderService.getOffenderSummaryByCrn(crn)
}

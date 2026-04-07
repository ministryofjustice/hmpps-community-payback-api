package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminUpwDetailsController(
  private val adjustmentsService: AdjustmentService,
  private val offenderService: OffenderService,
  private val contextService: ContextService,
) {

  @GetMapping(
    path = ["/offenders/{crn}/unpaid-work-details/{deliusEventNumber}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with unpaid work details summary",
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
  fun getEvent(@PathVariable crn: String, @PathVariable deliusEventNumber: Int) = offenderService.getUnpaidWorkDetails(
    upwDetailsId = UnpaidWorkDetailsIdDto(crn, deliusEventNumber),
    userName = contextService.getUserName(),
  ) ?: notFound("Unpaid Work Details", "CRN $crn, Event Number $deliusEventNumber")

  @PostMapping(
    path = ["/offenders/{crn}/unpaid-work-details/{deliusEventNumber}/adjustments"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjustment has been created",
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
  @Tag(name = "supportsIdempotencyKey")
  fun createAdjustment(
    @PathVariable crn: String,
    @PathVariable deliusEventNumber: Int,
    @Valid @RequestBody createAdjustment: CreateAdjustmentDto,
  ) = adjustmentsService.createAdjustment(
    upwDetailsId = UnpaidWorkDetailsIdDto(
      crn = crn,
      deliusEventNumber = deliusEventNumber,
    ),
    createAdjustment = createAdjustment,
    username = contextService.getUserName(),
  )
}

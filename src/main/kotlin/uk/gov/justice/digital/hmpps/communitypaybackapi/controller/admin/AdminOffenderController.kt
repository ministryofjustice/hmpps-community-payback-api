package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDomain
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminOffenderController(private val offenderService: OffenderService, private val contextService: ContextService) {
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
  fun getOffenderSummary(@PathVariable crn: String): CaseDetailsSummaryDto = offenderService.getOffenderSummaryByCrn(crn, contextService.getUserName())
    ?: notFound("Offender Summary", crn)

  @GetMapping(
    path = ["/offenders/{crn}/personal-circumstances"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get all personal circumstances by CRN, optionally filtered by type=TRAVEL_TIME",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response with a list of personal circumstances",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Unsupported personal circumstances type",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
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
  fun getPersonalCircumstances(
    @PathVariable crn: String,
    @RequestParam(required = false) type: String?,
  ): List<PersonalCircumstancesDto> {
    val filter = type?.let { value ->
      PersonalCircumstancesTypeDto.entries.firstOrNull { it.name == value }
        ?: throw BadRequestException("Unsupported personal circumstances type '$value'. Supported type: ${PersonalCircumstancesTypeDto.entries.joinToString()}")
    }
    return offenderService.getPersonalCircumstances(crn, filter?.toDomain()) ?: notFound("Personal Circumstances", crn)
  }
}

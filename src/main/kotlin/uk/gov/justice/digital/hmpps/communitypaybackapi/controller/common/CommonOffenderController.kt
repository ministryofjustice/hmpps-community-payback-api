package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CommonCaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toCommonDto

@CommonController
@RequestMapping(
  "/common/offenders",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class CommonOffenderController(private val offenderService: OffenderService, private val contextService: ContextService) {
  @GetMapping("/{crn}/summary")
  @Operation(description = "Get offender summary by CRN")
  fun getOffenderSummary(@PathVariable crn: String): CommonCaseDetailsSummaryDto = offenderService.getOffenderSummaryByCrn(crn, contextService.getUserName())?.toCommonDto()
    ?: notFound("Offender Summary", crn)

  @GetMapping("/{crn}/riskSummary")
  fun getRisk(@PathVariable crn: String): String = offenderService.getRiskSummary(crn) ?: notFound("Risk Summary", crn)
}

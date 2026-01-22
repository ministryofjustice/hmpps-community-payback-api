package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService

/*
This is a temporary controller added to prove ARNS integration
 */
@CommonController
@RequestMapping(
  "/common/offender",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class CommonOffenderController(val offenderService: OffenderService) {
  @GetMapping("/{crn}/riskSummary")
  fun getRisk(@PathVariable crn: String): String = offenderService.getRiskSummary(crn)
}

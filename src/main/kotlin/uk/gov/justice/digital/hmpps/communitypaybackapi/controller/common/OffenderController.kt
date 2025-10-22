package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService

/*
This is a temporary controller added to prove ARNS integration
 */
@CommonController
class OffenderController(val offenderService: OffenderService) {
  @GetMapping("/common/offender/{crn}/riskSummary")
  fun getRisk(@PathVariable crn: String): String = offenderService.getRiskSummary(crn)
}

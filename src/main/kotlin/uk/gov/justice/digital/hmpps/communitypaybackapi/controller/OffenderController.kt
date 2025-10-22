package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService

@CommunityPaybackController
class OffenderController(val offenderService: OffenderService) {
  @GetMapping("/offender/{crn}/riskSummary")
  fun getRisk(@PathVariable crn: String): String = offenderService.getRiskSummary(crn)
}

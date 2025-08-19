package uk.gov.justice.digital.hmpps.communitypaybackapi.example

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class Example(val apiName: String)

@RestController
@PreAuthorize("hasRole('ROLE_PROBATION')")
class ExampleController {
  @GetMapping("/example")
  fun getExample(): Example = Example(apiName = "hmpps-community-payback-api")
}

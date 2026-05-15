package uk.gov.justice.digital.hmpps.communitypaybackapi.client

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface ProbationAccessControlClient {
  @PostExchange("/user/{userName}/access")
  fun getAccessControlForCrns(
    @PathVariable userName: String,
    @RequestBody crns: List<String>,
  ): NDCaseAccess
}

data class NDCaseAccess(
  val access: List<NDCaseAccessItem>,
) {
  companion object
}

data class NDCaseAccessItem(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String?,
  val restrictionMessage: String?,
) {
  companion object
}

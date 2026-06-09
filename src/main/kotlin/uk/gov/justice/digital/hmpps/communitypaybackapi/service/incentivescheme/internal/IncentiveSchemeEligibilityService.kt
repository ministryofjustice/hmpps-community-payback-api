package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import org.springframework.stereotype.Service

@Service
class IncentiveSchemeEligibilityService {
  @Suppress(
    // Suppress these warnings as this function is a deliberate stub until https://dsdmoj.atlassian.net/browse/PI-4220
    // has been completed.
    // TODO: Remove this `@Suppress` annotation when adding a proper implementation for this function.
    "detekt.ForbiddenComment",
    "detekt.FunctionOnlyReturningConstant",
    "unused",
  )
  fun isEligible(crn: String, deliusEventNumber: Int): Boolean = true
}

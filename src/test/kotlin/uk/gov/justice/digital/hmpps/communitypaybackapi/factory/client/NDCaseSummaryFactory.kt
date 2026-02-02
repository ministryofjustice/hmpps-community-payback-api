package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDName
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate

fun NDCaseSummary.Companion.valid() = NDCaseSummary(
  crn = String.random(8),
  name = NDName.valid(),
  dateOfBirth = LocalDate.now().minusDays(Long.random(365 * 18, (365 * 18) * (365 * 80))),
  currentRestriction = false,
  currentExclusion = false,
)

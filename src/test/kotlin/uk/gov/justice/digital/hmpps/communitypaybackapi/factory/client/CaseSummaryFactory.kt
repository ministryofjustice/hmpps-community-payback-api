package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.time.LocalDate

fun CaseSummary.Companion.valid() = CaseSummary(
  crn = String.Companion.random(8),
  name = Name.valid(),
  dateOfBirth = LocalDate.now().minusDays(Long.random(365 * 18, (365 * 18) * (365 * 80))),
)

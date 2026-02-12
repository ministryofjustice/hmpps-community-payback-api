package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import kotlin.random.Random

fun NDCaseDetailsSummary.Companion.valid() = NDCaseDetailsSummary(
  unpaidWorkDetails = listOf(NDCaseDetail.valid()),
)

fun NDCaseDetail.Companion.valid() = NDCaseDetail(
  eventNumber = Random.nextLong(1, 6),
  requiredMinutes = Random.nextLong(100, 301),
  completedMinutes = Random.nextLong(150, 250),
  adjustments = Random.nextLong(25, 51),
  completedEteMinutes = Random.nextLong(10, 41),
)

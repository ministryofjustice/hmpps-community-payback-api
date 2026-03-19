package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDMainOffence
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.random.Random

fun NDCaseDetailsSummary.Companion.valid() = NDCaseDetailsSummary(
  case = NDCaseSummary.Companion.valid(),
  unpaidWorkDetails = listOf(NDCaseDetail.valid()),
)

fun NDCaseDetail.Companion.valid() = NDCaseDetail(
  eventNumber = Random.nextLong(1, 6),
  sentenceDate = randomLocalDate(),
  requiredMinutes = Random.nextLong(100, 301),
  completedMinutes = Random.nextLong(150, 250),
  adjustments = Random.nextLong(25, 51),
  completedEteMinutes = Random.nextLong(10, 41),
  court = NDCodeDescription.Companion.valid(),
  mainOffence = NDMainOffence.Companion.valid(),
  eventOutcome = String.random(10),
  upwStatus = String.random(10),
  referralDate = randomLocalDate(),
  convictionDate = randomLocalDate(),
)

fun NDMainOffence.Companion.valid() = NDMainOffence(
  code = String.random(10),
  description = String.random(100),
  date = randomLocalDate(),
  count = Random.nextInt(1, 100),
)

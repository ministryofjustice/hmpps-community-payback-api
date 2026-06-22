package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDMainOffence
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementSubType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentiveSchemeEligibilityScope
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentiveSchemeEligibilityScopeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomPastLocalDate
import kotlin.random.Random

fun NDCaseDetailsSummary.Companion.valid() = NDCaseDetailsSummary(
  case = NDCaseSummary.Companion.valid(),
  unpaidWorkDetails = listOf(NDUpwDetails.valid()),
)

fun NDUpwDetails.Companion.valid() = NDUpwDetails(
  eventNumber = Random.nextInt(1, 6),
  sentenceDate = randomPastLocalDate(),
  requiredMinutes = Random.nextLong(100, 301),
  completedMinutes = Random.nextLong(150, 250),
  adjustments = Random.nextLong(25, 51),
  completedEteMinutes = Random.nextLong(10, 41),
  court = NDCodeDescription.Companion.valid(),
  mainOffence = NDMainOffence.Companion.valid(),
  eventOutcome = String.random(10),
  eventOutcomeCode = String.random(4),
  upwStatus = String.random(10),
  referralDate = randomLocalDate(),
  convictionDate = randomLocalDate(),
  unpaidWorkRequirements = listOf(NDRequirementSubType.valid()),
)

fun NDUpwDetails.Companion.valid(ctx: ApplicationContext): NDUpwDetails {
  val repository = ctx.getBean(IncentiveSchemeEligibilityScopeEntityRepository::class.java)
  val scopes = repository.findAll()
  val eventOutcomeCode = scopes.first { it.scope == IncentiveSchemeEligibilityScope.OUTCOME && it.isEligible }.code

  return NDUpwDetails.valid().copy(
    eventOutcomeCode = eventOutcomeCode,
    unpaidWorkRequirements = listOf(NDRequirementSubType.valid(ctx)),
  )
}

fun NDMainOffence.Companion.valid() = NDMainOffence(
  code = String.random(10),
  description = String.random(100),
  date = randomLocalDate(),
  count = Random.nextInt(1, 100),
)

fun NDRequirementSubType.Companion.valid() = NDRequirementSubType(
  subType = NDCodeDescription.valid(),
)

fun NDRequirementSubType.Companion.valid(ctx: ApplicationContext): NDRequirementSubType {
  val repository = ctx.getBean(IncentiveSchemeEligibilityScopeEntityRepository::class.java)
  val scopes = repository.findAll()
  val requirementSubtypeCode = scopes.first { it.scope == IncentiveSchemeEligibilityScope.REQUIREMENT_SUBTYPE && it.isEligible }.code

  return NDRequirementSubType.valid().copy(
    subType = NDCodeDescription.valid().copy(code = requirementSubtypeCode),
  )
}

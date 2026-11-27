package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeStatus
import java.time.Duration
import kotlin.math.max
import kotlin.math.roundToLong

@Service
class IncentivesSchemeDiscountService(
  private val config: IncentivesSchemeConfig,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getDiscountProjection(
    requirement: NDUnpaidWorkRequirement,
    progression: IncentivesSchemeProgression,
  ): IncentivesSchemeDiscountDetails {
    logger.debug("Qualifying threshold: {}", config.getQualifyingThreshold(requirement))
    logger.debug("Maximum possible discount: {}", config.getMaximumDiscount(requirement))

    val result = IncentivesSchemeDiscountDetails(
      current = getCurrentDiscount(requirement, progression),
      projected = getProjectedDiscount(requirement, progression),
    )

    logger.debug("Current discount: {}", result.current)
    logger.debug("Projected discount: {}", result.projected)

    return result
  }

  private fun getProjectedDiscount(requirement: NDUnpaidWorkRequirement, progression: IncentivesSchemeProgression): Duration = when (progression.status) {
    IncentivesSchemeStatus.INELIGIBLE -> Duration.ZERO
    IncentivesSchemeStatus.DISQUALIFIED -> getCurrentDiscount(requirement, progression)
    else -> config.getMaximumDiscount(requirement)
  }

  private fun getCurrentDiscount(requirement: NDUnpaidWorkRequirement, progression: IncentivesSchemeProgression): Duration = Duration.ofMinutes(
    max(
      0.0,
      config.discountAwardedForQualifyingTimeRatio * (progression.qualifyingTimeWorked.toMinutes() - config.getQualifyingThreshold(requirement).toMinutes()),
    ).roundToLong(),
  )
}

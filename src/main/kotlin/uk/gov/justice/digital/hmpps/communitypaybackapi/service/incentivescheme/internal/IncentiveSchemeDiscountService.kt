package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import java.time.Duration
import kotlin.math.max
import kotlin.math.roundToLong

@Service
class IncentiveSchemeDiscountService(
  private val config: IncentiveSchemeConfig,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getDiscountProjection(
    requirement: NDUnpaidWorkRequirement,
    progression: IncentiveSchemeProgression,
  ): IncentiveSchemeDiscountDetails {
    logger.debug("Qualifying threshold: {}", config.getQualifyingThreshold(requirement))
    logger.debug("Maximum possible discount: {}", config.getMaximumDiscount(requirement))

    val result = IncentiveSchemeDiscountDetails(
      current = getCurrentDiscount(requirement, progression),
      projected = getProjectedDiscount(requirement, progression),
    )

    logger.debug("Current discount: {}", result.current)
    logger.debug("Projected discount: {}", result.projected)

    return result
  }

  private fun getProjectedDiscount(requirement: NDUnpaidWorkRequirement, progression: IncentiveSchemeProgression): Duration = when (progression.status) {
    IncentiveSchemeStatus.INELIGIBLE -> Duration.ZERO
    IncentiveSchemeStatus.DISQUALIFIED -> getCurrentDiscount(requirement, progression)
    else -> config.getMaximumDiscount(requirement)
  }

  private fun getCurrentDiscount(requirement: NDUnpaidWorkRequirement, progression: IncentiveSchemeProgression): Duration = Duration.ofMinutes(
    max(
      0.0,
      config.discountAwardedForQualifyingTimeRatio * (progression.qualifyingTimeWorked.toMinutes() - config.getQualifyingThreshold(requirement).toMinutes()),
    ).roundToLong(),
  )
}

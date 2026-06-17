package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import java.time.Duration
import kotlin.math.roundToLong

/**
 * Contains the common Spring configuration parameters for the Incentive Scheme.
 * See [Incentive Scheme Calculations](/docs/incentive-scheme/calculation.md) for more details on what these
 * configuration parameters represent.
 *
 * It provides some convenience functions to get [Duration]s for a specific [NDUnpaidWorkRequirement]:
 * - [getQualifyingThreshold]
 * - [getMaximumDiscount]
 */
@Configuration
data class IncentiveSchemeConfig(
  @param:Value("\${incentive-scheme.qualifying-time-threshold:0.25}") val qualifyingTimeThresholdRatio: Double,
  @param:Value("\${incentive-scheme.discount-awarded-for-qualifying-time:0.5}") val discountAwardedForQualifyingTimeRatio: Double,
) {
  /**
   * Returns the amount of time that must be worked in order to qualify in the Incentive Scheme for the given
   * [NDUnpaidWorkRequirement].
   */
  fun getQualifyingThreshold(requirement: NDUnpaidWorkRequirement): Duration = Duration.ofMinutes((requirement.requirementProgress.requiredMinutes * qualifyingTimeThresholdRatio).roundToLong())

  /**
   * Returns the maximum allowed time that could be discounted for the given [NDUnpaidWorkRequirement].
   *
   * See [Incentive Scheme Calculations](/docs/incentive-scheme/calculation.md) for details on how this is calculated.
   */
  fun getMaximumDiscount(requirement: NDUnpaidWorkRequirement): Duration = Duration.ofMinutes((requirement.requirementProgress.requiredMinutes * maximumDiscountRatio).roundToLong())

  /**
   * Returns the maximum allowed time that could be discounted as a proportion of the total requirement.
   *
   * See [Incentive Scheme Calculations](/docs/incentive-scheme/calculation.md) for details on how this is calculated.
   */
  val maximumDiscountRatio: Double
    get() = 1 - (1 + discountAwardedForQualifyingTimeRatio * qualifyingTimeThresholdRatio) / (1 + discountAwardedForQualifyingTimeRatio)
}

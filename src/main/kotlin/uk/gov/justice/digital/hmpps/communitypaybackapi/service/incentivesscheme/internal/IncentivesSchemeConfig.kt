package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import java.time.Duration
import kotlin.math.roundToLong

/**
 * Contains the common Spring configuration parameters for the Incentives Scheme.
 * See [Incentives Scheme Calculations](/docs/incentives-scheme/calculation.md) for more details on what these
 * configuration parameters represent.
 *
 * It provides some convenience functions to get [Duration]s for a specific [NDUnpaidWorkRequirement]:
 * - [getQualifyingThreshold]
 * - [getMaximumDiscount]
 */
@Configuration
data class IncentivesSchemeConfig(
  @param:Value("\${incentives-scheme.qualifying-time-threshold:0.25}") val qualifyingTimeThresholdRatio: Double,
  @param:Value("\${incentives-scheme.discount-awarded-for-qualifying-time:0.5}") val discountAwardedForQualifyingTimeRatio: Double,
) {
  /**
   * Returns the amount of time that must be worked in order to qualify in the Incentives Scheme for the given
   * [NDUnpaidWorkRequirement].
   */
  fun getQualifyingThreshold(requirement: NDUnpaidWorkRequirement): Duration = Duration.ofMinutes((requirement.requirementProgress.requiredMinutes * qualifyingTimeThresholdRatio).roundToLong())

  /**
   * Returns the maximum allowed time that could be discounted for the given [NDUnpaidWorkRequirement].
   *
   * See [Incentives Scheme Calculations](/docs/incentives-scheme/calculation.md) for details on how this is calculated.
   */
  fun getMaximumDiscount(requirement: NDUnpaidWorkRequirement): Duration = Duration.ofMinutes((requirement.requirementProgress.requiredMinutes * maximumDiscountRatio).roundToLong())

  /**
   * Returns the maximum allowed time that could be discounted as a proportion of the total requirement.
   *
   * See [Incentives Scheme Calculations](/docs/incentives-scheme/calculation.md) for details on how this is calculated.
   */
  val maximumDiscountRatio: Double
    get() = 1 - (1 + discountAwardedForQualifyingTimeRatio * qualifyingTimeThresholdRatio) / (1 + discountAwardedForQualifyingTimeRatio)
}

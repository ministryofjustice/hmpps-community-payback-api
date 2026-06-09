package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeConfig
import java.time.Duration

class IncentiveSchemeConfigTest {
  @ParameterizedTest
  @CsvSource(
    // 25% threshold, 60 hr requirement -> 15 hr threshold
    "0.25,60,PT15H",
    // 10% threshold, 100 hr requirement -> 10 hr threshold
    "0.1,100,PT10H",
  )
  fun `getQualifyingThreshold calculates the expected duration for the requirement`(
    qualifyingTimeThresholdRatio: Double,
    requiredHours: Int,
    expectedQualifyingThreshold: Duration,
  ) {
    val config = IncentiveSchemeConfig(qualifyingTimeThresholdRatio, 0.0)

    val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = requiredHours * 60)
    val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

    assertThat(config.getQualifyingThreshold(requirement)).isEqualTo(expectedQualifyingThreshold)
  }

  @ParameterizedTest
  @CsvSource(
    // 25% threshold, 30 min/hr award, 60 hr requirement -> 15 hr maximum discount
    "0.25,0.5,60,PT15H",
    // 10% threshold, 60 min/hr award, 100 hr requirement -> 45 hr maximum discount
    "0.1,1.0,100,PT45H",
  )
  fun `getMaximumDiscount calculates the expected duration for the requirement`(
    qualifyingTimeThresholdRatio: Double,
    discountAwardedForQualifyingTimeRatio: Double,
    requiredHours: Int,
    expectedMaximumDiscount: Duration,
  ) {
    val config = IncentiveSchemeConfig(qualifyingTimeThresholdRatio, discountAwardedForQualifyingTimeRatio)

    val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = requiredHours * 60)
    val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

    assertThat(config.getMaximumDiscount(requirement)).isEqualTo(expectedMaximumDiscount)
  }

  @ParameterizedTest
  @CsvSource(
    // 25% threshold, 30 min/hr award -> 25% maximum discount
    "0.25,0.5,0.25",
    // 10% threshold, 60 min/hr award -> 45% maximum discount
    "0.1,1.0,0.45",
  )
  fun `maximumDiscountRatio calculates the expected proportion`(
    qualifyingTimeThresholdRatio: Double,
    discountAwardedForQualifyingTimeRatio: Double,
    expectedMaximumDiscountRatio: Double,
  ) {
    val config = IncentiveSchemeConfig(qualifyingTimeThresholdRatio, discountAwardedForQualifyingTimeRatio)

    assertThat(config.maximumDiscountRatio).isCloseTo(expectedMaximumDiscountRatio, Offset.offset(0.01))
  }
}

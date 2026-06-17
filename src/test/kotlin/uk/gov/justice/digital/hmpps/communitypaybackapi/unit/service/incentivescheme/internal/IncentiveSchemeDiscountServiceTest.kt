package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeDiscountService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgression
import java.time.Duration

@ExtendWith(MockKExtension::class)
class IncentiveSchemeDiscountServiceTest {
  @Suppress("unused") // Injected by MockK
  private val config = IncentiveSchemeConfig(0.25, 0.5)

  @InjectMockKs
  private lateinit var service: IncentiveSchemeDiscountService

  @Test
  fun `Returns zero for current discount and projected discount when ineligible`() {
    val requirement = NDUnpaidWorkRequirement.valid()

    val result = service.getDiscountProjection(requirement, IncentiveSchemeProgression.INELIGIBLE)

    assertThat(result.current).isEqualTo(Duration.ZERO)
    assertThat(result.projected).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `Returns current discount of zero and projected discount of the maximum available when threshold is not met`() {
    val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
    val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)
    val progression = IncentiveSchemeProgression(Duration.ofHours(20), IncentiveSchemeStatus.ELIGIBLE)

    val result = service.getDiscountProjection(requirement, progression)

    assertThat(result.current).isEqualTo(Duration.ZERO)
    assertThat(result.projected).isEqualTo(Duration.ofHours(25))
  }

  @ParameterizedTest
  @CsvSource(
    "PT26H,PT30M",
    "PT35H,PT5H",
    "PT50H,PT12H30M",
    "PT75H,PT25H",
  )
  fun `Returns earned current discount and projected discount of the maximum available when threshold is met`(
    qualifyingTimeWorked: Duration,
    expectedCurrentDiscount: Duration,
  ) {
    val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
    val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)
    val progression = IncentiveSchemeProgression(qualifyingTimeWorked, IncentiveSchemeStatus.QUALIFYING)

    val result = service.getDiscountProjection(requirement, progression)

    assertThat(result.current).isEqualTo(expectedCurrentDiscount)
    assertThat(result.projected).isEqualTo(Duration.ofHours(25))
  }

  @Test
  fun `Returns earned current discount as the projected discount when disqualified`() {
    val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
    val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)
    val progression = IncentiveSchemeProgression(Duration.ofHours(49), IncentiveSchemeStatus.DISQUALIFIED)

    val result = service.getDiscountProjection(requirement, progression)

    assertThat(result.current).isEqualTo(Duration.ofHours(12))
    assertThat(result.projected).isEqualTo(Duration.ofHours(12))
  }
}

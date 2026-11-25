package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivesscheme

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivesscheme.validAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeDiscountDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeDiscountService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEligibilityService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeProgression
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeProgressionService
import java.time.Duration

@ExtendWith(MockKExtension::class)
class IncentivesSchemeServiceTest {
  @MockK
  lateinit var incentivesSchemeDiscountService: IncentivesSchemeDiscountService

  @MockK
  lateinit var incentivesSchemeEligibilityService: IncentivesSchemeEligibilityService

  @MockK
  lateinit var incentivesSchemeEventService: IncentivesSchemeEventService

  @MockK
  lateinit var incentivesSchemeProgressionService: IncentivesSchemeProgressionService

  @MockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK
  lateinit var config: IncentivesSchemeConfig

  @InjectMockKs
  lateinit var service: IncentivesSchemeService

  companion object {
    private const val CRN = "X123456"
    private const val EVENT_NUMBER = 1
  }

  @Nested
  inner class GetDetails {
    @Test
    fun `returns expected result when ineligible`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentivesSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentivesSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(8)
      every { incentivesSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns false
      every { incentivesSchemeDiscountService.getDiscountProjection(requirement, IncentivesSchemeProgression.INELIGIBLE) } returns IncentivesSchemeDiscountDetails(
        Duration.ZERO,
        Duration.ZERO,
      )
      every { config.getQualifyingThreshold(requirement) } returns Duration.ofHours(25)

      val result = service.getDetails(CRN, EVENT_NUMBER)

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(8))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.currentDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.INELIGIBLE)
    }

    @Test
    fun `returns expected result when disqualified`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8), isDisqualifying = true),
      )

      val progression = IncentivesSchemeProgression(Duration.ofHours(32), IncentivesSchemeStatus.DISQUALIFIED)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentivesSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentivesSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(40)
      every { incentivesSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentivesSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentivesSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentivesSchemeDiscountDetails(
        Duration.ofMinutes(3 * 60 + 30),
        Duration.ofMinutes(3 * 60 + 30),
      )
      every { config.getQualifyingThreshold(requirement) } returns Duration.ofHours(25)

      val result = service.getDetails(CRN, EVENT_NUMBER)

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(32))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(40))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ofMinutes(3 * 60 + 30))
      assertThat(result.currentDiscount).isEqualTo(Duration.ofMinutes(3 * 60 + 30))
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.DISQUALIFIED)
    }

    @Test
    fun `returns expected result when eligible`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val progression = IncentivesSchemeProgression(Duration.ofHours(16), IncentivesSchemeStatus.ELIGIBLE)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentivesSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentivesSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(16)
      every { incentivesSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentivesSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentivesSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentivesSchemeDiscountDetails(
        Duration.ZERO,
        Duration.ofHours(25),
      )
      every { config.getQualifyingThreshold(requirement) } returns Duration.ofHours(25)

      val result = service.getDetails(CRN, EVENT_NUMBER)

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(16))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(16))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ofHours(25))
      assertThat(result.currentDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `returns expected result when qualifying`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentivesSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val progression = IncentivesSchemeProgression(Duration.ofHours(32), IncentivesSchemeStatus.QUALIFYING)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentivesSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentivesSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(32)
      every { incentivesSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentivesSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentivesSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentivesSchemeDiscountDetails(
        Duration.ofMinutes(3 * 60 + 30),
        Duration.ofHours(25),
      )
      every { config.getQualifyingThreshold(requirement) } returns Duration.ofHours(25)

      val result = service.getDetails(CRN, EVENT_NUMBER)

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(32))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(32))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ofHours(25))
      assertThat(result.currentDiscount).isEqualTo(Duration.ofMinutes(3 * 60 + 30))
      assertThat(result.status).isEqualTo(IncentivesSchemeStatus.QUALIFYING)
    }
  }

  @Nested
  inner class GetMetadata {
    @ParameterizedTest
    @CsvSource(
      "0.1,1,0.45,10%,100%,45%",
      "0.25,0.5,0.25,25%,50%,25%",
      "0.3333,0.61616,0.7777,33.3%,61.6%,77.8%",
    )
    fun `Returns config parameters as percentages`(
      qualifyingTimeThresholdRatio: Double,
      discountAwardedForQualifyingTimeRatio: Double,
      maximumDiscountRatio: Double,
      expectedQualifyingThresholdPercentage: String,
      expectedDiscountPercentage: String,
      expectedMaximumDiscountPercentage: String,
    ) {
      every { config.qualifyingTimeThresholdRatio } returns qualifyingTimeThresholdRatio
      every { config.discountAwardedForQualifyingTimeRatio } returns discountAwardedForQualifyingTimeRatio
      every { config.maximumDiscountRatio } returns maximumDiscountRatio

      val result = service.getMetadata()

      assertThat(result.qualifyingThresholdPercentage).isEqualTo(expectedQualifyingThresholdPercentage)
      assertThat(result.discountPercentage).isEqualTo(expectedDiscountPercentage)
      assertThat(result.maximumDiscountPercentage).isEqualTo(expectedMaximumDiscountPercentage)
    }
  }
}

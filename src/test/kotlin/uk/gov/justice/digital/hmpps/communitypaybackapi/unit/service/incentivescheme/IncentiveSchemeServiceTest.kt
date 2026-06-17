package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivescheme.validAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeDiscountDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeDiscountService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEligibilityService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgression
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgressionService
import java.time.Duration

@ExtendWith(MockKExtension::class)
class IncentiveSchemeServiceTest {
  @MockK
  lateinit var incentiveSchemeDiscountService: IncentiveSchemeDiscountService

  @MockK
  lateinit var incentiveSchemeEligibilityService: IncentiveSchemeEligibilityService

  @MockK
  lateinit var incentiveSchemeEventService: IncentiveSchemeEventService

  @MockK
  lateinit var incentiveSchemeProgressionService: IncentiveSchemeProgressionService

  @MockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK
  lateinit var config: IncentiveSchemeConfig

  @InjectMockKs
  lateinit var service: IncentiveSchemeService

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
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentiveSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentiveSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(8)
      every { incentiveSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns false
      every { incentiveSchemeDiscountService.getDiscountProjection(requirement, IncentiveSchemeProgression.INELIGIBLE) } returns IncentiveSchemeDiscountDetails(
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
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.INELIGIBLE)
    }

    @Test
    fun `returns expected result when disqualified`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8), isDisqualifying = true),
      )

      val progression = IncentiveSchemeProgression(Duration.ofHours(32), IncentiveSchemeStatus.DISQUALIFIED)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentiveSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentiveSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(40)
      every { incentiveSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentiveSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentiveSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentiveSchemeDiscountDetails(
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
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.DISQUALIFIED)
    }

    @Test
    fun `returns expected result when eligible`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val progression = IncentiveSchemeProgression(Duration.ofHours(16), IncentiveSchemeStatus.ELIGIBLE)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentiveSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentiveSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(16)
      every { incentiveSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentiveSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentiveSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentiveSchemeDiscountDetails(
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
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `returns expected result when qualifying`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val events = listOf(
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
        IncentiveSchemeEvent.validAppointment(duration = Duration.ofHours(8)),
      )

      val progression = IncentiveSchemeProgression(Duration.ofHours(32), IncentiveSchemeStatus.QUALIFYING)

      every { communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn = CRN, eventNumber = EVENT_NUMBER) } returns requirement
      every { incentiveSchemeEventService.getEvents(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns events
      every { incentiveSchemeProgressionService.getTotalTimeWorked(events) } returns Duration.ofHours(32)
      every { incentiveSchemeEligibilityService.isEligible(crn = CRN, deliusEventNumber = EVENT_NUMBER) } returns true
      every { incentiveSchemeProgressionService.getProgress(requirement, events) } returns progression
      every { incentiveSchemeDiscountService.getDiscountProjection(requirement, progression) } returns IncentiveSchemeDiscountDetails(
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
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.QUALIFYING)
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

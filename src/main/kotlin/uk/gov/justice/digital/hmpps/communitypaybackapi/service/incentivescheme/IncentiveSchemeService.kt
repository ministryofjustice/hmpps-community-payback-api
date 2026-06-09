package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeDiscountService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEligibilityService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgression
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeProgressionService
import java.text.DecimalFormat
import java.time.Duration

@Service
class IncentiveSchemeService(
  private val incentiveSchemeDiscountService: IncentiveSchemeDiscountService,
  private val incentiveSchemeEligibilityService: IncentiveSchemeEligibilityService,
  private val incentiveSchemeEventService: IncentiveSchemeEventService,
  private val incentiveSchemeProgressionService: IncentiveSchemeProgressionService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val config: IncentiveSchemeConfig,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val metadataPercentageFormat = DecimalFormat("0%").apply { maximumFractionDigits = 1 }

  fun getDetails(crn: String, deliusEventNumber: Int): IncentiveSchemeDetails {
    val requirement = communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn, deliusEventNumber)
    val events = incentiveSchemeEventService.getEvents(crn, deliusEventNumber)

    logger.debug("Requirement: {}", requirement.requiredTime)

    val totalTimeWorked = incentiveSchemeProgressionService.getTotalTimeWorked(events)
    val progression = if (incentiveSchemeEligibilityService.isEligible(crn, deliusEventNumber)) {
      logger.debug("Eligible for incentive scheme")
      incentiveSchemeProgressionService.getProgress(requirement, events)
    } else {
      logger.debug("Not eligible for incentive scheme")
      IncentiveSchemeProgression.INELIGIBLE
    }

    val (currentDiscount, projectedDiscount) = incentiveSchemeDiscountService.getDiscountProjection(requirement, progression)

    return IncentiveSchemeDetails(
      totalRequirement = requirement.requiredTime,
      qualifyingThreshold = config.getQualifyingThreshold(requirement),
      qualifyingTimeWorked = progression.qualifyingTimeWorked,
      totalTimeWorked = totalTimeWorked,
      projectedDiscount = projectedDiscount,
      currentDiscount = currentDiscount,
      status = progression.status,
    )
  }

  fun getMetadata() = IncentiveSchemeMetadata(
    qualifyingThresholdPercentage = metadataPercentageFormat.format(config.qualifyingTimeThresholdRatio),
    discountPercentage = metadataPercentageFormat.format(config.discountAwardedForQualifyingTimeRatio),
    maximumDiscountPercentage = metadataPercentageFormat.format(config.maximumDiscountRatio),
  )

  private val NDUnpaidWorkRequirement.requiredTime: Duration
    get() = Duration.ofMinutes(requirementProgress.requiredMinutes.toLong())
}

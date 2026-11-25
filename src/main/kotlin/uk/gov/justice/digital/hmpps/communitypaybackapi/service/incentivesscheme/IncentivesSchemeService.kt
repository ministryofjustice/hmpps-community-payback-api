package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeDiscountService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEligibilityService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEventService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeProgression
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeProgressionService
import java.text.DecimalFormat
import java.time.Duration

@Service
class IncentivesSchemeService(
  private val incentivesSchemeDiscountService: IncentivesSchemeDiscountService,
  private val incentivesSchemeEligibilityService: IncentivesSchemeEligibilityService,
  private val incentivesSchemeEventService: IncentivesSchemeEventService,
  private val incentivesSchemeProgressionService: IncentivesSchemeProgressionService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val config: IncentivesSchemeConfig,
) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val metadataPercentageFormat = DecimalFormat("0%").apply { maximumFractionDigits = 1 }

  fun getDetails(crn: String, deliusEventNumber: Int): IncentivesSchemeDetails {
    val requirement = communityPaybackAndDeliusClient.getUnpaidWorkRequirement(crn, deliusEventNumber)
    val events = incentivesSchemeEventService.getEvents(crn, deliusEventNumber)

    logger.debug("Requirement: {}", requirement.requiredTime)

    val totalTimeWorked = incentivesSchemeProgressionService.getTotalTimeWorked(events)
    val progression = if (incentivesSchemeEligibilityService.isEligible(crn, deliusEventNumber)) {
      logger.debug("Eligible for incentives scheme")
      incentivesSchemeProgressionService.getProgress(requirement, events)
    } else {
      logger.debug("Not eligible for incentives scheme")
      IncentivesSchemeProgression.INELIGIBLE
    }

    val (currentDiscount, projectedDiscount) = incentivesSchemeDiscountService.getDiscountProjection(requirement, progression)

    return IncentivesSchemeDetails(
      totalRequirement = requirement.requiredTime,
      qualifyingThreshold = config.getQualifyingThreshold(requirement),
      qualifyingTimeWorked = progression.qualifyingTimeWorked,
      totalTimeWorked = totalTimeWorked,
      projectedDiscount = projectedDiscount,
      currentDiscount = currentDiscount,
      status = progression.status,
    )
  }

  fun getMetadata() = IncentivesSchemeMetadata(
    qualifyingThresholdPercentage = metadataPercentageFormat.format(config.qualifyingTimeThresholdRatio),
    discountPercentage = metadataPercentageFormat.format(config.discountAwardedForQualifyingTimeRatio),
    maximumDiscountPercentage = metadataPercentageFormat.format(config.maximumDiscountRatio),
  )

  private val NDUnpaidWorkRequirement.requiredTime: Duration
    get() = Duration.ofMinutes(requirementProgress.requiredMinutes.toLong())
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.IncentivesSchemeStatus
import java.time.Duration

@Service
class IncentivesSchemeProgressionService(
  private val config: IncentivesSchemeConfig,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getProgress(requirement: NDUnpaidWorkRequirement, events: List<IncentivesSchemeEvent>): IncentivesSchemeProgression {
    val qualifyingThreshold = config.getQualifyingThreshold(requirement)
    logger.debug("Qualifying threshold: {}", qualifyingThreshold)

    val qualifyingTimeWorked = getQualifyingTimeWorked(events)
    val disqualifyingEvent = events.firstOrNull { it.isDisqualifying }

    val status = when {
      disqualifyingEvent != null -> {
        logger.debug("Event {} is disqualifying", disqualifyingEvent.name)
        logger.debug("Disqualified from Incentives Scheme")
        IncentivesSchemeStatus.DISQUALIFIED
      }
      qualifyingTimeWorked >= qualifyingThreshold -> {
        logger.debug("Threshold was reached")
        IncentivesSchemeStatus.QUALIFYING
      }
      else -> {
        logger.debug("Threshold was not reached yet")
        IncentivesSchemeStatus.ELIGIBLE
      }
    }

    return IncentivesSchemeProgression(qualifyingTimeWorked, status)
  }

  private fun getQualifyingTimeWorked(events: List<IncentivesSchemeEvent>): Duration {
    val qualifyingTimeWorked = events
      .takeWhile { !it.isDisqualifying }
      .fold(Duration.ZERO) { acc, event ->
        if (event.isQualifying) {
          logger.debug("Crediting {} from qualifying event {}", event.duration, event.name)
          acc + event.duration
        } else {
          logger.debug("Event {} does not qualify for credited time, skipping", event.name)
          acc
        }
      }
      .coerceAtLeast(Duration.ZERO)

    logger.debug("Time worked in qualifying events: {}", qualifyingTimeWorked)

    return qualifyingTimeWorked
  }

  fun getTotalTimeWorked(events: List<IncentivesSchemeEvent>): Duration {
    val totalTimeWorked = events.map { it.duration }
      .fold(Duration.ZERO, Duration::plus)
      .coerceAtLeast(Duration.ZERO)

    logger.debug("Total time worked: {}", totalTimeWorked)

    return totalTimeWorked
  }
}

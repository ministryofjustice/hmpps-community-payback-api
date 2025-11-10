package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration

/**
 * Used where a duration in the 'HH:MM' format is required in JSON
 */
@Schema(type = "string", example = "02:30", description = "Duration formatted as HH:MM")
data class HourMinuteDuration(
  val duration: Duration,
)

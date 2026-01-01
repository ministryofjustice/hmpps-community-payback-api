package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration

/**
 * Used where a duration in the 'HH:MM' format is required in JSON
 */
@Schema(type = "string", example = "02:30", description = "Duration formatted as HH:MM")
data class HourMinuteDuration(
  val duration: Duration,
) {
  companion object {
    @JvmStatic
    @JsonCreator
    fun fromString(value: String?): HourMinuteDuration? {
      if (value.isNullOrBlank()) return null

      val split = value.split(":")
      require(split.size == 2) { "Duration must be in HH:MM format" }

      val duration = Duration.ofHours(split[0].toLong()).plusMinutes(split[1].toLong())
      return HourMinuteDuration(duration)
    }
  }

  @JsonValue
  override fun toString(): String {
    val hours = duration.toHours().toString().padStart(2, '0')
    val minutes = duration.toMinutesPart().toString().padStart(2, '0')
    return "$hours:$minutes"
  }
}

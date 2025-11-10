package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import java.time.Duration

class HourMinuteDurationDeserializer : JsonDeserializer<HourMinuteDuration?>() {
  override fun deserialize(
    p: JsonParser,
    ctxt: DeserializationContext,
  ): HourMinuteDuration? {
    val value: JsonNode? = p.codec.readTree(p)
    if (value == null || value.asText().isNullOrEmpty()) {
      return null
    }
    val split = value.asText().split(":")
    return HourMinuteDuration(Duration.ofHours(split[0].toLong()).plusMinutes(split[1].toLong()))
  }
}

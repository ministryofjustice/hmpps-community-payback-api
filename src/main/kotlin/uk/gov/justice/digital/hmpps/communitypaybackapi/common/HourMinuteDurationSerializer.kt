package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class HourMinuteDurationSerializer : JsonSerializer<HourMinuteDuration>() {
  override fun serialize(
    value: HourMinuteDuration,
    gen: JsonGenerator,
    serializers: SerializerProvider,
  ) {
    gen.writeString("${value.duration.toHoursPart().pad()}:${value.duration.toMinutesPart().pad()}")
  }

  private fun Int.pad() = this.toString().padStart(2, '0')
}

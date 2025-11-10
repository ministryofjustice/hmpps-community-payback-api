package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.JacksonCustomConfig
import java.time.Duration

class HourMinuteDurationSerializeDeserializeTest {

  val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JacksonCustomConfig().customModule())

  data class TimeDurationContainer(
    val myDuration: HourMinuteDuration?,
  )

  @Nested
  inner class Serialize {

    @Test
    fun `serializes value correctly`() {
      val result = objectMapper.writeValueAsString(
        TimeDurationContainer(
          myDuration = HourMinuteDuration(Duration.ofHours(23).plusMinutes(59)),
        ),
      )

      assertThat(result).isEqualTo("{\"myDuration\":\"23:59\"}")
    }

    @Test
    fun `serializes null correctly`() {
      val result = objectMapper.writeValueAsString(
        TimeDurationContainer(
          myDuration = null,
        ),
      )

      assertThat(result).isEqualTo("{\"myDuration\":null}")
    }
  }

  @Nested
  inner class Deserialize {

    @Test
    fun `deserializes value correctly`() {
      val result = objectMapper.readValue(
        "{\"myDuration\":\"23:59\"}",
        TimeDurationContainer::class.java,
      )

      assertThat(result).isEqualTo(
        TimeDurationContainer(
          myDuration = HourMinuteDuration(Duration.ofHours(23).plusMinutes(59)),
        ),
      )
    }

    @Test
    fun `deserializes null correctly`() {
      val result = objectMapper.readValue(
        "{\"myDuration\":null}",
        TimeDurationContainer::class.java,
      )

      assertThat(result).isEqualTo(
        TimeDurationContainer(
          myDuration = null,
        ),
      )
    }

    @Test
    fun `deserializes empty string correctly`() {
      val result = objectMapper.readValue(
        "{\"myDuration\":\"\"}",
        TimeDurationContainer::class.java,
      )

      assertThat(result).isEqualTo(
        TimeDurationContainer(
          myDuration = null,
        ),
      )
    }
  }
}

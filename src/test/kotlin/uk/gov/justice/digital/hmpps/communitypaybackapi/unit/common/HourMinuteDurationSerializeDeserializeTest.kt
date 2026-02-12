package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
import java.time.Duration

class HourMinuteDurationSerializeDeserializeTest {

  val jsonMapper: JsonMapper = JsonMapper()

  data class TimeDurationContainer(
    val myDuration: HourMinuteDuration?,
  )

  @Nested
  inner class Serialize {

    @Test
    fun `serializes value correctly`() {
      val result = jsonMapper.writeValueAsString(
        TimeDurationContainer(
          myDuration = HourMinuteDuration(Duration.ofHours(23).plusMinutes(59)),
        ),
      )

      assertThat(result).isEqualTo("{\"myDuration\":\"23:59\"}")
    }

    @Test
    fun `serializes null correctly`() {
      val result = jsonMapper.writeValueAsString(
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
      val result = jsonMapper.readValue(
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
      val result = jsonMapper.readValue(
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
      val result = jsonMapper.readValue(
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

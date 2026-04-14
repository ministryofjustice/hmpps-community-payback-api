package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.SanitizingStringDeserializer

class SanitizingStringDeserializerTest {
  @Test
  fun `sanitise html`() {
    val result = jacksonObjectMapper().readValue(
      """
        {"notes": "a note with some script<script>alert('hello')</script>"}
      """.trimIndent(),
      MyRequestDto::class.java,
    )

    assertThat(result.notes).isEqualTo("a note with some script")
  }

  data class MyRequestDto(
    @field:JsonDeserialize(using = SanitizingStringDeserializer::class)
    val notes: String,
  )
}

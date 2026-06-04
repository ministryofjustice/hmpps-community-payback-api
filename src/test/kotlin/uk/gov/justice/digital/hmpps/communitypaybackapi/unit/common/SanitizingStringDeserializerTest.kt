package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.databind.DatabindException
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.SanitizingStringDeserializer

class SanitizingStringDeserializerTest {
  @ParameterizedTest
  @CsvSource(
    value = [
      "a note with some script<script>alert('hello')</script>,Tag '<script>' is not allowed",
      "a note with a <a href='https://example.com/'>link</a>,Tag '<a>' is not allowed",
      "a note with <h1>big text</h1>,Tag '<h1>' is not allowed",
    ],
  )
  fun `validates that html tags are not present`(notes: String, expectedMessage: String) {
    val ex = assertThrows(DatabindException::class.java) {
      jacksonObjectMapper().readValue(
        """
          {"notes": "$notes"}
        """.trimIndent(),
        MyRequestDto::class.java,
      )
    }

    assertThat(ex).hasMessageStartingWith(expectedMessage)
  }

  @Test
  fun `does not HTML encode the resulting string`() {
    val result = jacksonObjectMapper().readValue(
      """
        {"notes": "don't encode the single quote or these <>@&"}
      """.trimIndent(),
      MyRequestDto::class.java,
    )

    assertThat(result.notes).isEqualTo("don't encode the single quote or these <>@&")
  }

  data class MyRequestDto(
    @field:JsonDeserialize(using = SanitizingStringDeserializer::class)
    val notes: String,
  )
}

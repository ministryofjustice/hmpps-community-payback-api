package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class PersonalCircumstancesMapperTest {
  @ParameterizedTest
  @ValueSource(strings = ["", "\"startDate\": null,"])
  fun `deserializes and maps circumstances with an unknown start date`(startDateField: String) {
    val circumstances = jacksonObjectMapper().readValue<List<NDPersonalCircumstances>>(
      """
      [{
        $startDateField
        "type": { "code": "K", "description": "Travel" },
        "subType": { "code": "K09", "description": "Travel time" },
        "notes": "Travel arrangement"
      }]
      """.trimIndent(),
    )

    val result = circumstances.toDto()

    assertThat(result).hasSize(1)
    assertThat(result.single().startDate).isNull()
    assertThat(result.single().type.code).isEqualTo("K")
    assertThat(result.single().subType?.code).isEqualTo("K09")
    assertThat(result.single().notes).isEqualTo("Travel arrangement")
  }

  @Test
  fun `maps every circumstance including nullable fields`() {
    val circumstances = listOf(
      NDPersonalCircumstances.valid("K", "K09").copy(verified = true),
      NDPersonalCircumstances.valid("K", "K09").copy(verified = false),
      NDPersonalCircumstances.valid("A", null).copy(verified = null, notes = null, endDate = null),
    )

    val result = circumstances.toDto()

    assertThat(result).hasSize(3)
    result.zip(circumstances).forEach { (actual, expected) ->
      assertThat(actual.type.code).isEqualTo(expected.type.code)
      assertThat(actual.type.description).isEqualTo(expected.type.description)
      assertThat(actual.subType?.code).isEqualTo(expected.subType?.code)
      assertThat(actual.subType?.description).isEqualTo(expected.subType?.description)
      assertThat(actual.startDate).isEqualTo(expected.startDate)
      assertThat(actual.endDate).isEqualTo(expected.endDate)
      assertThat(actual.verified).isEqualTo(expected.verified)
      assertThat(actual.notes).isEqualTo(expected.notes)
    }
  }

  @Test
  fun `maps an empty list`() {
    assertThat(emptyList<NDPersonalCircumstances>().toDto()).isEmpty()
  }
}

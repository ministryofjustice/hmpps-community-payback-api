package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class PersonalCircumstancesMapperTest {
  @Test
  fun `maps every circumstance including nullable fields`() {
    val circumstances = listOf(
      NDPersonalCircumstances.valid("K", "K09"),
      NDPersonalCircumstances.valid("A", null).copy(verified = null, notes = null, endDate = null),
    )

    val result = circumstances.toDto()

    assertThat(result).hasSize(2)
    result.zip(circumstances).forEach { (actual, expected) ->
      assertThat(actual.type.code).isEqualTo(expected.type.code)
      assertThat(actual.type.description).isEqualTo(expected.type.description)
      assertThat(actual.subType?.code).isEqualTo(expected.subType?.code)
      assertThat(actual.subType?.description).isEqualTo(expected.subType?.description)
      assertThat(actual.startDate).isEqualTo(expected.startDate)
      assertThat(actual.endDate).isEqualTo(expected.endDate)
      assertThat(actual.verified).isEqualTo(expected.verified ?: false)
      assertThat(actual.notes).isEqualTo(expected.notes)
    }
  }

  @Test
  fun `maps an empty list`() {
    assertThat(emptyList<NDPersonalCircumstances>().toDto()).isEmpty()
  }
}

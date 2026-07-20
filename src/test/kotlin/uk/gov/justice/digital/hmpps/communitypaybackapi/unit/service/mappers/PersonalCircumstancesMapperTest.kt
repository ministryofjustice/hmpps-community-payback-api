package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class PersonalCircumstancesMapperTest {
  @Test
  fun `returns when travel time is allowed`() {
    val personalCircumstances = listOf(NDPersonalCircumstances.valid("K", "K09")).toDto()

    assertThat(personalCircumstances.isAllowedTravelTime).isTrue
  }

  @Test
  fun `returns default with travel time not allowed`() {
    val personalCircumstances = listOf(NDPersonalCircumstances.valid()).toDto()

    assertThat(personalCircumstances.isAllowedTravelTime).isFalse
  }
}

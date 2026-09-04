package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class PersonalCircumstancesMapperTest {
  @Test
  fun `returns when travel time is allowed`() {
    val travelTime = NDPersonalCircumstances.valid("K", "K09")
    val personalCircumstances = listOf(travelTime).toDto()

    assertThat(personalCircumstances.isAllowedTravelTime).isTrue
    assertThat(personalCircumstances.travelTimeDetails).isNotNull
    assertThat(personalCircumstances.travelTimeDetails!!.startDate).isEqualTo(travelTime.startDate)
    assertThat(personalCircumstances.travelTimeDetails.endDate).isEqualTo(travelTime.endDate)
    assertThat(personalCircumstances.travelTimeDetails.verified).isEqualTo(travelTime.verified)
    assertThat(personalCircumstances.travelTimeDetails.notes).isEqualTo(travelTime.notes)
  }

  @Test
  fun `returns default with travel time not allowed`() {
    val personalCircumstances = listOf(NDPersonalCircumstances.valid()).toDto()

    assertThat(personalCircumstances.isAllowedTravelTime).isFalse
    assertThat(personalCircumstances.travelTimeDetails).isNull()
  }
}

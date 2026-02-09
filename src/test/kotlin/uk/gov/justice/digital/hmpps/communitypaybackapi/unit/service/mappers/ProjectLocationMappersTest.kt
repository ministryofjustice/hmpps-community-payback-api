package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPickUpLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class ProjectLocationMappersTest {

  @Nested
  inner class NDAddressToDto {

    @Test
    fun success() {
      val result = NDAddress(
        buildingName = "build 1",
        addressNumber = "15a",
        streetName = "bracklow close",
        townCity = "madeupville",
        county = "fakeorton",
        postCode = "AB12 123",
      ).toDto()

      assertThat(result.buildingName).isEqualTo("build 1")
      assertThat(result.buildingNumber).isEqualTo("15a")
      assertThat(result.streetName).isEqualTo("bracklow close")
      assertThat(result.townCity).isEqualTo("madeupville")
      assertThat(result.county).isEqualTo("fakeorton")
      assertThat(result.postCode).isEqualTo("AB12 123")
    }
  }

  @Nested
  inner class NDPickUpLocationToDto {

    @Test
    fun success() {
      val result = NDPickUpLocation(
        code = "CD123",
        description = "A location",
        buildingName = "build 1",
        addressNumber = "15a",
        streetName = "bracklow close",
        townCity = "madeupville",
        county = "fakeorton",
        postCode = "AB12 123",
      ).toDto()

      assertThat(result.buildingName).isEqualTo("build 1")
      assertThat(result.buildingNumber).isEqualTo("15a")
      assertThat(result.streetName).isEqualTo("bracklow close")
      assertThat(result.townCity).isEqualTo("madeupville")
      assertThat(result.county).isEqualTo("fakeorton")
      assertThat(result.postCode).isEqualTo("AB12 123")
    }
  }
}

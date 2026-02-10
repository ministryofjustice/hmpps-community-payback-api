package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class ProjectMappersTest {

  @Test
  fun `should map NDProject to ProjectSummaryDto correctly`() {
    val ndAddress = NDAddress(
      buildingName = "Justice Building",
      addressNumber = "123",
      streetName = "Main Street",
      townCity = "London",
      county = "Greater London",
      postCode = "SW1A 1AA",
    )

    val ndProject = NDProject(
      name = "Community Garden Maintenance",
      code = "PRJ123",
      location = ndAddress,
      overdueOutcomesCount = 5,
      oldestOverdueInDays = 10,
    )

    val result = ndProject.toDto()

    assertThat(result.projectName).isEqualTo("Community Garden Maintenance")
    assertThat(result.projectCode).isEqualTo("PRJ123")
    assertThat(result.location.buildingName).isEqualTo("Justice Building")
    assertThat(result.location.buildingNumber).isEqualTo("123")
    assertThat(result.location.streetName).isEqualTo("Main Street")
    assertThat(result.location.townCity).isEqualTo("London")
    assertThat(result.location.county).isEqualTo("Greater London")
    assertThat(result.location.postCode).isEqualTo("SW1A 1AA")
    assertThat(result.numberOfAppointmentsOverdue).isEqualTo(5)
    assertThat(result.oldestOverdueAppointmentInDays).isEqualTo(10)
  }
}

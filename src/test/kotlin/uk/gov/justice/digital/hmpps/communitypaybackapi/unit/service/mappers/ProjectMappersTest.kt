package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

class ProjectMappersTest {

  @Nested
  inner class NDProjectToDto {

    @Test
    fun `should map NDProject to ProjectDto correctly`() {
      val projectTypeEntity = ProjectTypeEntity.valid()
      val location = NDAddress.valid()
      val beneficiaryLocation = NDAddress.valid()

      val result = NDProject(
        name = "The Project Name",
        code = "The Project Code",
        type = NDProjectType.valid(),
        provider = NDCode("provider code"),
        team = NDCode("team code"),
        location = location,
        beneficiaryDetails = NDBeneficiaryDetails(
          name = "ben name",
          contactName = "ben contact",
          emailAddress = "ben email",
          website = "ben website",
          telephoneNumber = "ben tel",
          location = beneficiaryLocation,
        ),
        hiVisRequired = false,
      ).toDto(projectTypeEntity)

      assertThat(result.projectName).isEqualTo("The Project Name")
      assertThat(result.projectCode).isEqualTo("The Project Code")
      assertThat(result.projectType).isEqualTo(projectTypeEntity.toDto())
      assertThat(result.teamCode).isEqualTo("team code")
      assertThat(result.projectType).isEqualTo(projectTypeEntity.toDto())
      assertThat(result.location).isEqualTo(location.toDto())
      assertThat(result.beneficiaryDetails.beneficiary).isEqualTo("ben name")
      assertThat(result.beneficiaryDetails.contactName).isEqualTo("ben contact")
      assertThat(result.beneficiaryDetails.emailAddress).isEqualTo("ben email")
      assertThat(result.beneficiaryDetails.website).isEqualTo("ben website")
      assertThat(result.beneficiaryDetails.telephoneNumber).isEqualTo("ben tel")
      assertThat(result.beneficiaryDetails.location).isEqualTo(beneficiaryLocation.toDto())
      assertThat(result.hiVisRequired).isFalse
    }
  }

  @Nested
  inner class NDProjectOutcomeSummaryToDto {

    @Test
    fun `should map NDProjectOutcomeSummary to ProjectOutcomeSummaryDto correctly`() {
      val ndAddress = NDAddress(
        buildingName = "Justice Building",
        addressNumber = "123",
        streetName = "Main Street",
        townCity = "London",
        county = "Greater London",
        postCode = "SW1A 1AA",
      )

      val ndProject = NDProjectOutcomeSummary(
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
}

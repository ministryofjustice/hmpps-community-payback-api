package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BeneficiaryDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity

fun NDProject.toDto(
  projectType: ProjectTypeEntity,
) = ProjectDto(
  projectName = this.name,
  projectCode = this.code,
  projectType = projectType.toDto(),
  location = this.location.toDto(),
  hiVisRequired = this.hiVisRequired,
  beneficiaryDetails = this.beneficiaryDetails.toDto(),
)

fun NDBeneficiaryDetails.toDto() = BeneficiaryDetailsDto(
  beneficiary = this.beneficiary,
  contactName = this.contactName,
  emailAddress = this.emailAddress,
  website = this.website,
  telephoneNumber = this.telephoneNumber,
  location = location.toDto(),
)

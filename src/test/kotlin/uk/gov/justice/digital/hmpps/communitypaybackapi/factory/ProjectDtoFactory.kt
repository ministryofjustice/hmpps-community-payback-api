package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BeneficiaryDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import kotlin.String

fun ProjectDto.Companion.valid() = ProjectDto(
  projectName = String.random(50),
  projectCode = String.random(5),
  projectType = ProjectTypeDto.valid(),
  location = LocationDto.valid(),
  hiVisRequired = Boolean.random(),
  beneficiaryDetails = BeneficiaryDetailsDto.valid(),
)

fun LocationDto.Companion.valid() = LocationDto(
  buildingName = String.random(50),
  buildingNumber = String.random(50),
  streetName = String.random(50),
  townCity = String.random(50),
  county = String.random(50),
  postCode = String.random(5),
)

fun BeneficiaryDetailsDto.Companion.valid() = BeneficiaryDetailsDto(
  beneficiary = String.random(50),
  contactName = String.random(50),
  emailAddress = String.random(50),
  website = String.random(50),
  telephoneNumber = String.random(50),
  location = LocationDto.valid(),
)

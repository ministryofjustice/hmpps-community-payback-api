package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BeneficiaryDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectAvailabilityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingDayOfWeekDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingFrequencyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.String

fun ProjectDto.Companion.valid() = ProjectDto(
  projectName = String.Companion.random(50),
  projectCode = String.random(5),
  projectType = ProjectTypeDto.valid(),
  providerCode = String.random(5),
  teamCode = String.random(5),
  location = LocationDto.valid(),
  hiVisRequired = Boolean.random(),
  beneficiaryDetails = BeneficiaryDetailsDto.valid(),
  expectedEndDateExclusive = randomLocalDate(),
  actualEndDateExclusive = randomLocalDate(),
  availability = listOf(
    ProjectAvailabilityDto.valid(),
  ),
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

fun ProjectAvailabilityDto.Companion.valid() = ProjectAvailabilityDto(
  frequency = SchedulingFrequencyDto.entries.random(),
  dayOfWeek = SchedulingDayOfWeekDto.entries.random(),
  startDateInclusive = randomLocalDate(),
  endDateExclusive = randomLocalDate(),
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesCodeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.PersonalCircumstancesType

fun List<NDPersonalCircumstances>.toDto(): List<PersonalCircumstancesDto> = map { it.toDto() }

private fun NDPersonalCircumstances.toDto(): PersonalCircumstancesDto = PersonalCircumstancesDto(
  type = PersonalCircumstancesCodeDto(type.code, type.description),
  subType = subType?.let { PersonalCircumstancesCodeDto(it.code, it.description) },
  startDate = startDate,
  endDate = endDate,
  verified = verified,
  notes = notes,
)

fun PersonalCircumstancesTypeDto.toDomain(): PersonalCircumstancesType = when (this) {
  PersonalCircumstancesTypeDto.TRAVEL_TIME -> PersonalCircumstancesType.TRAVEL_TIME
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesDto

fun List<NDPersonalCircumstances>.toDto(): PersonalCircumstancesDto = PersonalCircumstancesDto(
  travelTimeDetails = this.get(PersonalCircumstances.ALLOWED_TRAVEL_TIME)?.toDto(),
)

private fun NDPersonalCircumstances.toDto(): PersonalCircumstancesDetailsDto = PersonalCircumstancesDetailsDto(
  startDate = this.startDate,
  endDate = this.endDate,
  verified = this.verified ?: false,
  notes = this.notes,
)

private fun List<NDPersonalCircumstances>.get(personalCircumstances: PersonalCircumstances): NDPersonalCircumstances? = this.firstOrNull {
  it.type.code == personalCircumstances.typeCode && it.subType?.code == personalCircumstances.subTypeCode
}

private enum class PersonalCircumstances(val typeCode: String, val subTypeCode: String?) {
  ALLOWED_TRAVEL_TIME("K", "K09"),
}

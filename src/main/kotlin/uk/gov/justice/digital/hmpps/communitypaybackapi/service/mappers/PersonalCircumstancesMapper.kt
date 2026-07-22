package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDPersonalCircumstances
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PersonalCircumstancesDto

fun List<NDPersonalCircumstances>.toDto(): PersonalCircumstancesDto = PersonalCircumstancesDto(
  isAllowedTravelTime = this.contains(PersonalCircumstances.ALLOWED_TRAVEL_TIME),
)

private fun List<NDPersonalCircumstances>.contains(personalCircumstances: PersonalCircumstances): Boolean = this.any {
  it.type.code == personalCircumstances.typeCode && it.subType?.code == personalCircumstances.subTypeCode
}

private enum class PersonalCircumstances(val typeCode: String, val subTypeCode: String?) {
  ALLOWED_TRAVEL_TIME("K", "K09"),
}

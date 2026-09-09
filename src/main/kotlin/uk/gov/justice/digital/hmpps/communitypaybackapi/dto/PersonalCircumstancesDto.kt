package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.OffsetDateTime

data class PersonalCircumstancesDto(
  val type: PersonalCircumstancesCodeDto,
  val subType: PersonalCircumstancesCodeDto?,
  val startDate: OffsetDateTime,
  val endDate: OffsetDateTime?,
  val verified: Boolean?,
  val notes: String?,
)

data class PersonalCircumstancesCodeDto(
  val code: String,
  val description: String,
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "objectType",
)
@JsonSubTypes(
  JsonSubTypes.Type(OffenderDto.OffenderNotFoundDto::class, name = "Not_Found"),
  JsonSubTypes.Type(OffenderDto.OffenderLimitedDto::class, name = "Limited"),
  JsonSubTypes.Type(OffenderDto.OffenderFullDto::class, name = "Full"),
)
sealed interface OffenderDto {
  val crn: String

  data class OffenderNotFoundDto(
    override val crn: String,
  ) : OffenderDto

  data class OffenderLimitedDto(
    override val crn: String,
  ) : OffenderDto

  data class OffenderFullDto(
    override val crn: String,
    val forename: String,
    val surname: String,
    val middleNames: List<String>,
  ) : OffenderDto
}

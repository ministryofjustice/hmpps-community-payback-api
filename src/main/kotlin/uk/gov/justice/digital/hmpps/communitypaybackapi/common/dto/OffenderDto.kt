package uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto.OffenderFullDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto.OffenderLimitedDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto.OffenderNotFoundDto

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "objectType",
)
@JsonSubTypes(
  JsonSubTypes.Type(OffenderNotFoundDto::class, name = "Not_Found"),
  JsonSubTypes.Type(OffenderLimitedDto::class, name = "Limited"),
  JsonSubTypes.Type(OffenderFullDto::class, name = "Full"),
)
@Schema(
  requiredMode = Schema.RequiredMode.REQUIRED,
  discriminatorProperty = "objectType",
  discriminatorMapping = [
    DiscriminatorMapping(value = "Not_Found", schema = OffenderNotFoundDto::class),
    DiscriminatorMapping(value = "Limited", schema = OffenderLimitedDto::class),
    DiscriminatorMapping(value = "Full", schema = OffenderFullDto::class),
  ],
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto.OffenderFullDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto.OffenderLimitedDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto.OffenderNotFoundDto
import java.time.LocalDate

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "objectType",
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
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
  val objectType: OffenderType

  data class OffenderNotFoundDto(
    override val crn: String,
  ) : OffenderDto {
    override val objectType = OffenderType.NOT_FOUND
  }

  data class OffenderLimitedDto(
    override val crn: String,
  ) : OffenderDto {
    override val objectType = OffenderType.LIMITED
  }

  data class OffenderFullDto(
    override val crn: String,
    val forename: String,
    val surname: String,
    val middleNames: List<String>,
    val dateOfBirth: LocalDate,
  ) : OffenderDto {
    override val objectType = OffenderType.FULL
  }
}

enum class OffenderType(@get:JsonValue val value: String) {
  NOT_FOUND("Not_Found"),
  LIMITED("Limited"),
  FULL("Full"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String) = entries.first { it.value == value }
  }
}

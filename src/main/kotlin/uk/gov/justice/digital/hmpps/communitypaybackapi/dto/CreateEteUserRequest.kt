package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateEteUserRequest(
  @field:NotBlank(message = "CRN is required")
  val crn: String,

  @field:NotBlank(message = "Email is required")
  @field:Email(message = "Must be a valid email format")
  val email: String,
)

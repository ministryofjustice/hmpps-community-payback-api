package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentReasonDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun AdjustmentReasonDto.Companion.valid() = AdjustmentReasonDto(
  id = UUID.randomUUID(),
  name = String.random(20),
  deliusCode = String.random(5),
  maxMinutesAllowed = Int.random(60, 480),
)

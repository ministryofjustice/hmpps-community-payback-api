package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import org.testcontainers.utility.Base58.randomString
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomPastLocalDate
import java.util.UUID.randomUUID

fun AdjustmentDto.Companion.valid() = AdjustmentDto(
  deliusId = Long.random(),
  id = randomUUID(),
  date = randomPastLocalDate(),
  amount = randomDuration(),
  reason = randomString(50),
  reasonCode = randomString(4),
)

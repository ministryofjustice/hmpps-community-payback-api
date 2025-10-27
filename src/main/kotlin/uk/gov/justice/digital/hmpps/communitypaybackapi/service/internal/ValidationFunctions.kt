package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> validateNotNull(value: T?, lazyMessage: () -> Any): T {
  contract {
    returns() implies (value != null)
  }

  if (value == null) {
    val message = lazyMessage()
    throw BadRequestException(message.toString())
  } else {
    return value
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import java.time.LocalDate
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

inline fun validateNotInPast(date: LocalDate?, lazyMessage: (LocalDate) -> Any) {
  date?.let { providedDate ->
    if (providedDate.isBefore(LocalDate.now())) {
      val message = lazyMessage(providedDate)
      throw BadRequestException(message.toString())
    }
  }
}

inline fun validateLengthLessThan(
  value: String?,
  maxExclusive: Int,
  lazyMessage: (actualLength: Int, maxExclusive: Int) -> Any,
) {
  value?.let { provided ->
    val length = provided.length
    if (length > maxExclusive) {
      val message = lazyMessage(length, maxExclusive)
      throw BadRequestException(message.toString())
    }
  }
}

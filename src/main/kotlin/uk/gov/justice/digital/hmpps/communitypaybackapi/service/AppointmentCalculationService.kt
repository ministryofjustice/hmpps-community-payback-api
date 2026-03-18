package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import java.time.Duration
import java.time.LocalTime

@Service
class AppointmentCalculationService {
  fun minutesToCredit(
    contactOutcome: ContactOutcomeEntity?,
    startTime: LocalTime,
    endTime: LocalTime,
    penaltyMinutes: Duration?,
  ): Duration? {
    if (contactOutcome?.attended != true) return null

    val minutesCredited = Duration.between(startTime, endTime).minus(penaltyMinutes ?: Duration.ZERO)
    return minutesCredited.takeIf { it != Duration.ZERO }
  }
}

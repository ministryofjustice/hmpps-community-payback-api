package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivescheme

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent
import java.time.Duration

fun IncentiveSchemeEvent.Companion.validAppointment(
  duration: Duration? = null,
  isQualifying: Boolean = true,
  isDisqualifying: Boolean = false,
) = IncentiveSchemeAppointmentEvent(
  inner = AppointmentSummaryDto.valid()
    .let { if (duration != null) it.copy(minutesCredited = duration.toMinutes()) else it }
    .let { if (isQualifying) it.copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = false)) else it.copy(contactOutcome = null) }
    .let { if (isDisqualifying) it.copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = true)) else it },
)

fun IncentiveSchemeEvent.Companion.validAdjustment(duration: Duration? = null) = IncentiveSchemeAdjustmentEvent(
  inner = AdjustmentDto.valid()
    .let { if (duration != null) it.copy(amount = duration.negated()) else it },
)

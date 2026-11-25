package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.incentivesscheme

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEvent.IncentivesSchemeAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal.IncentivesSchemeEvent.IncentivesSchemeAppointmentEvent
import java.time.Duration

fun IncentivesSchemeEvent.Companion.validAppointment(
  duration: Duration? = null,
  isQualifying: Boolean = true,
  isDisqualifying: Boolean = false,
) = IncentivesSchemeAppointmentEvent(
  inner = AppointmentSummaryDto.valid()
    .let { if (duration != null) it.copy(minutesCredited = duration.toMinutes()) else it }
    .let { if (isQualifying) it.copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = false)) else it.copy(contactOutcome = null) }
    .let { if (isDisqualifying) it.copy(contactOutcome = ContactOutcomeDto.valid().copy(enforceable = true)) else it },
)

fun IncentivesSchemeEvent.Companion.validAdjustment(duration: Duration? = null) = IncentivesSchemeAdjustmentEvent(
  inner = AdjustmentDto.valid()
    .let { if (duration != null) it.copy(amount = duration.negated()) else it },
)

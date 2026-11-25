package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivesscheme.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId

sealed interface IncentivesSchemeEvent {
  val name: String
  val timestamp: OffsetDateTime
  val isDisqualifying: Boolean
  val isQualifying: Boolean
  val duration: Duration

  data class IncentivesSchemeAppointmentEvent(private val inner: AppointmentSummaryDto) : IncentivesSchemeEvent {
    override val name: String = "Appointment ${inner.id}"
    override val timestamp: OffsetDateTime = inner.date.atTime(inner.startTime).atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
    override val isDisqualifying: Boolean = inner.contactOutcome?.enforceable ?: false
    override val isQualifying: Boolean = inner.hasOutcome()
    override val duration: Duration = Duration.ofMinutes(inner.minutesCredited ?: 0)
  }

  data class IncentivesSchemeCourseCompletionAppointmentEvent(private val inner: AppointmentSummaryDto) : IncentivesSchemeEvent {
    override val name: String = "Course completion appointment ${inner.id}"
    override val timestamp: OffsetDateTime = inner.date.atTime(inner.startTime).atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
    override val isDisqualifying: Boolean = false
    override val isQualifying: Boolean = inner.hasOutcome()
    override val duration: Duration = Duration.ofMinutes(inner.minutesCredited ?: 0)
  }

  data class IncentivesSchemeAdjustmentEvent(private val inner: AdjustmentDto) : IncentivesSchemeEvent {
    override val name: String = "Adjustment ${inner.deliusId}"
    override val timestamp: OffsetDateTime = inner.date.atTime(23, 59, 59).atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
    override val isDisqualifying: Boolean = false
    override val isQualifying: Boolean = true
    override val duration: Duration = inner.amount.negated()
  }

  companion object
}

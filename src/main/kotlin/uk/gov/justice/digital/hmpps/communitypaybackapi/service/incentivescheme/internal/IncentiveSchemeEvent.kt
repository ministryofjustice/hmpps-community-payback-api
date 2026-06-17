package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId

sealed interface IncentiveSchemeEvent {
  val name: String
  val timestamp: OffsetDateTime
  val isDisqualifying: Boolean
  val isQualifying: Boolean
  val duration: Duration

  data class IncentiveSchemeAppointmentEvent(private val inner: AppointmentSummaryDto) : IncentiveSchemeEvent {
    override val name: String = "Appointment ${inner.id}"
    override val timestamp: OffsetDateTime = inner.date.atTime(inner.startTime).atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
    override val isDisqualifying: Boolean = inner.contactOutcome?.enforceable ?: false
    override val isQualifying: Boolean = inner.hasOutcome()
    override val duration: Duration = Duration.ofMinutes(inner.minutesCredited ?: 0)
  }

  data class IncentiveSchemeAdjustmentEvent(private val inner: AdjustmentDto) : IncentiveSchemeEvent {
    override val name: String = "Adjustment ${inner.deliusId}"
    override val timestamp: OffsetDateTime = inner.date.atTime(23, 59, 59).atZone(ZoneId.of("Europe/London")).toOffsetDateTime()
    override val isDisqualifying: Boolean = false
    override val isQualifying: Boolean = true
    override val duration: Duration = inner.amount.negated()
  }

  companion object {
    fun fromAppointmentsAndAdjustments(
      appointments: List<AppointmentSummaryDto>,
      adjustments: List<AdjustmentDto>,
    ): List<IncentiveSchemeEvent> {
      val appointmentEvents = appointments.map { IncentiveSchemeAppointmentEvent(it) }
      val adjustmentEvents = adjustments.map { IncentiveSchemeAdjustmentEvent(it) }

      return (adjustmentEvents + appointmentEvents).sortedBy { it.timestamp }
    }
  }
}

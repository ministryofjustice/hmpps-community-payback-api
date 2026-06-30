package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent
import java.time.LocalDate

@Service
class IncentiveSchemeEventService(
  private val appointmentService: AppointmentService,
  private val adjustmentService: AdjustmentService,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  fun getEvents(crn: String, deliusEventNumber: Int): List<IncentiveSchemeEvent> {
    val appointments = appointmentService.getAppointments(
      crn = crn,
      eventNumber = deliusEventNumber.toString(),
      toDate = LocalDate.now(),
      pageable = Pageable.unpaged(),
    ).toList()
    val adjustments = adjustmentService.getAdjustments(crn = crn, eventNumber = deliusEventNumber)

    val events = getEventsFromAppointmentsAndAdjustments(appointments, adjustments)

    logger.debug(
      "Found {} events for CRN {} and Delius event number {}:\n{}",
      events.size,
      crn,
      deliusEventNumber,
      events.joinToString("\n") { "${it.name}\t@ ${it.timestamp}:\t${it.duration}" },
    )

    return events
  }

  private fun getEventsFromAppointmentsAndAdjustments(
    appointments: List<AppointmentSummaryDto>,
    adjustments: List<AdjustmentDto>,
  ): List<IncentiveSchemeEvent> {
    val appointmentEvents = appointments.map { IncentiveSchemeAppointmentEvent(it) }
    val adjustmentEvents = adjustments.map { IncentiveSchemeAdjustmentEvent(it) }

    return (adjustmentEvents + appointmentEvents).sortedBy { it.timestamp }
  }
}

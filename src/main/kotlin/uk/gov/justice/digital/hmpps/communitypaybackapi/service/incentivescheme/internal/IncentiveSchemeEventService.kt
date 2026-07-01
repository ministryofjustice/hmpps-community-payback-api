package uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent
import java.time.LocalDate

@Service
class IncentiveSchemeEventService(
  private val appointmentService: AppointmentService,
  private val adjustmentService: AdjustmentService,
  private val eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository,
  private val projectTypeEntityRepository: ProjectTypeEntityRepository,
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
    val eteAppointmentIds = eteCourseCompletionEventResolutionRepository.existsByDeliusAppointmentId(appointments.map { it.id })
    val eteProjectTypeCodes = projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.ETE).map { it.code }

    val events = getEvents(appointments, adjustments, eteAppointmentIds, eteProjectTypeCodes)

    logger.debug(
      "Found {} events for CRN {} and Delius event number {}:\n{}",
      events.size,
      crn,
      deliusEventNumber,
      events.joinToString("\n") { "${it.name}\t@ ${it.timestamp}:\t${it.duration}" },
    )

    return events
  }

  private fun getEvents(
    appointments: List<AppointmentSummaryDto>,
    adjustments: List<AdjustmentDto>,
    eteAppointmentIds: List<Long>,
    eteProjectTypeCodes: List<String>,
  ): List<IncentiveSchemeEvent> {
    val appointmentEvents = appointments.map {
      if (eteAppointmentIds.contains(it.id) || eteProjectTypeCodes.contains(it.projectTypeCode)) {
        IncentiveSchemeCourseCompletionAppointmentEvent(it)
      } else {
        IncentiveSchemeAppointmentEvent(it)
      }
    }
    val adjustmentEvents = adjustments.map { IncentiveSchemeAdjustmentEvent(it) }

    return (adjustmentEvents + appointmentEvents).sortedBy { it.timestamp }
  }
}

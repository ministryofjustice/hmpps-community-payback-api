package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.ZoneId

@Service
class SarService(
  val appointmentEntityRepository: AppointmentEntityRepository,
  val appointmentEventEntityRepository: AppointmentEventEntityRepository,
  val eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository,
) : HmppsProbationSubjectAccessRequestService {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getProbationContentFor(
    crn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInclusive = fromDate?.atStartOfDay(ZoneId.systemDefault())?.toOffsetDateTime()
    val toDateTimeExclusive = toDate?.plusDays(1)?.atStartOfDay(ZoneId.systemDefault())?.toOffsetDateTime()

    log.debug("Subject Access Request for CRN '{}' from '{}' inclusive to '{}' exclusive", crn, fromDateInclusive, toDateTimeExclusive)

    val appointmentIds = appointmentEntityRepository.findAppointmentIdsWithEventsInRange(crn, fromDateInclusive, toDateTimeExclusive)
    val appointmentEntities = appointmentEntityRepository.findAllByIdOrderByDateAsc(appointmentIds)

    val courseCompletionEvents = eteCourseCompletionEventEntityRepository.findByCrnAndReceivedAt(
      crn = crn,
      fromDateInclusive = fromDateInclusive,
      toDateTimeExclusive = toDateTimeExclusive,
    )

    val appointments = appointmentEntities.map { appointment ->
      appointment.toSarEntry(
        appointmentEventEntityRepository.findByAppointmentOrderByCreatedAtAsc(appointment),
      )
    }

    if (appointments.isEmpty() && courseCompletionEvents.isEmpty()) {
      return null
    }

    return HmppsSubjectAccessRequestContent(
      content = mapOf(
        "appointments" to appointments,
        "eteCourseCompletionEvents" to courseCompletionEvents.map { completionEvent ->
          completionEvent.toSarEntry(
            appointmentEvent = completionEvent.resolution?.let { resolution ->
              appointmentEventEntityRepository.findByTriggerTypeAndTriggeredBy(
                triggerType = AppointmentEventTriggerType.ETE_COURSE_COMPLETION_RESOLUTION,
                triggeredBy = resolution.id.toString(),
              )
            },
          )
        },
      ),
      attachments = null,
    )
  }
}

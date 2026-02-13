package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.ZoneId

@Service
class SarService(
  val appointmentEventEntityRepository: AppointmentEventEntityRepository,
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

    val appointmentEvents = appointmentEventEntityRepository.findByCrnAndTriggeredAt(
      crn = crn,
      fromDateInclusive = fromDateInclusive,
      toDateTimeExclusive = toDateTimeExclusive,
    )

    if (appointmentEvents.isEmpty()) {
      return null
    }

    return HmppsSubjectAccessRequestContent(
      content = mapOf(
        "appointmentEvents" to appointmentEvents.map { it.toSarEntry() },
      ),
      attachments = null,
    )
  }
}

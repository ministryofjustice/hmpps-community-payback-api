package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.kotlin.sar.HmppsProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SarService : HmppsProbationSubjectAccessRequestService {
  override fun getProbationContentFor(
    crn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? = HmppsSubjectAccessRequestContent(
    content = emptyList<String>(),
    attachments = null,
  )
}

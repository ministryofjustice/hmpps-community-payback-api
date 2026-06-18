package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderSearchRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderSearchResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import java.util.UUID

@Service
class CourseCompletionAutoResolutionService(
  private val personSearchClient: ProbationOffenderSearchClient,
  private val draftResolutionRepository: EteCourseCompletionDraftResolutionRepository,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(CourseCompletionAutoResolutionService::class.java)
  }

  fun resolveAndPersistDraft(event: EteCourseCompletionEventEntity) {
    val crn = searchForCrn(event)
    draftResolutionRepository.save(
      EteCourseCompletionDraftResolutionEntity(
        id = UUID.randomUUID(),
        eteCourseCompletionEvent = event,
        crn = crn,
      ),
    )
  }

  private fun searchForCrn(event: EteCourseCompletionEventEntity): String? {
    val result = personSearchClient.searchPerson(
      OffenderSearchRequest(
        firstName = event.firstName,
        surname = event.lastName,
        dateOfBirth = event.dateOfBirth,
      ),
    ).let { OffenderSearchResult.from(it) }

    return when (result) {
      is OffenderSearchResult.SingleMatch -> {
        log.debug("CRN auto-resolved for event {}: {}", event.id, result.crn)
        result.crn
      }
      is OffenderSearchResult.NoMatch -> {
        log.debug("No CRN match for event {}", event.id)
        null
      }
      is OffenderSearchResult.MultipleMatches -> {
        log.debug("Ambiguous CRN (multiple matches) for event {}", event.id)
        null
      }
    }
  }
}

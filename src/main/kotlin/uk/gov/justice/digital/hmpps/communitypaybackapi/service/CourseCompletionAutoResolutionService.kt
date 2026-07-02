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
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingRepository
import java.util.UUID

@Service
class CourseCompletionAutoResolutionService(
  private val personSearchClient: ProbationOffenderSearchClient,
  private val officeUpwTeamMappingRepository: OfficeUpwTeamMappingRepository,
  private val courseCompletionProjectResolutionService: CourseCompletionProjectResolutionService,
  private val draftResolutionRepository: EteCourseCompletionDraftResolutionRepository,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(CourseCompletionAutoResolutionService::class.java)
  }

  fun getDraftResolutionForCourseCompletion(courseCompletionEventId: UUID): EteCourseCompletionDraftResolutionEntity? = draftResolutionRepository.findByEteCourseCompletionEventId(courseCompletionEventId)

  fun resolveAndPersistDraft(event: EteCourseCompletionEventEntity) {
    val crn = searchForCrn(event)
    val teamCode = resolveTeamCode(event)
    val projectCode = teamCode?.let { resolveProjectCode(event, it) }

    draftResolutionRepository.save(
      EteCourseCompletionDraftResolutionEntity(
        id = UUID.randomUUID(),
        eteCourseCompletionEvent = event,
        crn = crn,
        teamCode = teamCode,
        projectCode = projectCode,
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

  private fun resolveTeamCode(event: EteCourseCompletionEventEntity): String? {
    val mapping = officeUpwTeamMappingRepository.findByPduAndOffice(event.pdu, event.office)

    return mapping?.teamCode.also { teamCode ->
      when {
        teamCode != null -> log.debug("UPW team auto-resolved for event {}: {}", event.id, teamCode)
        mapping != null -> log.debug("Office mapping has no UPW team for event {}", event.id)
        else -> log.debug("No UPW team mapping for event {}", event.id)
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun resolveProjectCode(event: EteCourseCompletionEventEntity, teamCode: String): String? = try {
    courseCompletionProjectResolutionService.resolveProjectCode(event, teamCode)
  } catch (e: Exception) {
    log.warn("Project auto-resolution failed for event {}; project will be left blank", event.id, e)
    null
  }
}

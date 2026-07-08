package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderSearchRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OffenderSearchResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionDraftResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.OfficeUpwTeamMappingRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toHttpParams
import java.time.LocalDate
import java.util.UUID

@Service
class CourseCompletionAutoResolutionService(
  private val personSearchClient: ProbationOffenderSearchClient,
  private val officeUpwTeamMappingRepository: OfficeUpwTeamMappingRepository,
  private val courseCompletionProjectResolutionService: CourseCompletionProjectResolutionService,
  private val draftResolutionRepository: EteCourseCompletionDraftResolutionRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val projectTypeEntityRepository: ProjectTypeEntityRepository,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(CourseCompletionAutoResolutionService::class.java)
  }

  private val eteProjectTypeCodes by lazy { projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.ETE).map { it.code } }

  fun getDraftResolutionForCourseCompletion(courseCompletionEventId: UUID): EteCourseCompletionDraftResolutionEntity? = draftResolutionRepository.findByEteCourseCompletionEventId(courseCompletionEventId)

  fun resolveAndPersistDraft(event: EteCourseCompletionEventEntity) {
    val crn = searchForCrn(event)
    val teamCode = resolveTeamCode(event)
    val projectCode = teamCode?.let { resolveProjectCode(event, it) }

    val appointmentId = if (crn != null && projectCode != null) {
      log.info("CRN and project code auto-resolved, searching for matching appointments")
      searchForAppointment(crn, projectCode)
    } else {
      log.info("Could not match appointment")
      null
    }

    draftResolutionRepository.save(
      EteCourseCompletionDraftResolutionEntity(
        id = UUID.randomUUID(),
        eteCourseCompletionEvent = event,
        crn = crn,
        teamCode = teamCode,
        projectCode = projectCode,
        appointmentIdToUpdate = appointmentId,
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

  private fun searchForAppointment(crn: String, projectCode: String): Long? {
    val eventNumber = getEventNumberForCrn(crn) ?: return null

    val candidateAppointments = communityPaybackAndDeliusClient.getAppointments(
      username = "",
      crn = crn,
      fromDate = LocalDate.now(),
      toDate = null,
      outcomeCodes = listOf("NO_OUTCOME"),
      projectCodes = listOf(projectCode),
      projectTypeCodes = eteProjectTypeCodes,
      eventNumber = eventNumber,
      appointmentIds = null,
      params = Pageable.unpaged().toHttpParams(), // Unsorted because the PI API does not support sorting by both date and start time
    ).content
      // Perform the sorting that was unable to be done through the client.
      .sortedWith(compareBy<NDAppointmentSummary> { it.date }.thenBy { it.startTime })

    log.info("Found candidate appointments: {}", candidateAppointments.map { it.id })

    val appointmentId = candidateAppointments.firstOrNull()?.id

    log.info("Soonest appointment without contact outcome: {}", appointmentId)

    return appointmentId
  }

  private fun getEventNumberForCrn(crn: String): String? {
    val upwDetails = communityPaybackAndDeliusClient.getUpwDetailsSummary(crn, null).unpaidWorkDetails
    return when (upwDetails.size) {
      1 -> {
        val result = upwDetails.first().eventNumber.toString()
        log.debug("Event number auto-resolved for CRN {}: {}", crn, result)

        result
      }
      0 -> {
        log.debug("No events for CRN {}", crn)
        null
      }
      else -> {
        log.debug("Ambiguous event number (multiple matches) for CRN {}", crn)
        null
      }
    }
  }
}

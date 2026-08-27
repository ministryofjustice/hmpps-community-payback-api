package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.asPage
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toMultiValueHttpParams
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import java.time.LocalDate

@Service
class AppointmentRetrievalService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val appointmentMappers: AppointmentMappers,
  private val contextService: ContextService,
  private val projectService: ProjectService,
  private val offenderService: OffenderService,
  private val appointmentEntityRepository: AppointmentEntityRepository,
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun getAppointment(id: DeliusAppointmentIdDto): AppointmentDto? = try {
    communityPaybackAndDeliusClient.getAppointment(
      projectCode = id.projectCode,
      appointmentId = id.deliusAppointmentId,
      username = contextService.getUserName(),
    ).let { appointment ->
      val projectTypeCode = appointment.projectType.code
      val projectType = projectService.getProjectTypeForCode(projectTypeCode) ?: error("Can't resolve project type for code $projectTypeCode")

      val appointmentEntity = appointmentEntityRepository.findByDeliusId(appointment.id)

      appointmentMappers.toDto(appointment, appointmentEntity, projectType)
    }
  } catch (_: WebClientResponseException.NotFound) {
    null
  }

  fun getAppointments(
    crn: String? = null,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    outcomeCodes: List<String>? = null,
    projectCodes: List<String>? = null,
    projectTypeGroup: List<ProjectTypeGroupDto>? = null,
    eventNumber: String? = null,
    deliusAppointmentIds: List<Long>? = null,
    pageable: Pageable,
  ): Page<AppointmentSummaryDto> {
    val pageResponse = communityPaybackAndDeliusClient.getAppointments(
      username = contextService.getUserName(),
      crn = crn,
      fromDate = fromDate,
      toDate = toDate,
      outcomeCodes = expandOutcomeCodes(outcomeCodes),
      projectCodes = projectCodes,
      projectTypeCodes = projectTypeGroup?.flatMap { group -> projectService.projectTypesForGroup(group).map { it.code } },
      eventNumber = eventNumber,
      appointmentIds = deliusAppointmentIds,
      params = pageable.toMultiValueHttpParams(),
    )
    return pageResponse.asPage(pageable) { appointmentMappers.toSummaryDto(it) }
  }

  private fun expandOutcomeCodes(outcomeCodes: List<String>?): List<String>? = outcomeCodes?.flatMap { outcomeCode ->
    if (outcomeCode == WITH_OUTCOME) {
      contactOutcomeEntityRepository.findAll().map { it.code }
    } else {
      listOf(outcomeCode)
    }
  }?.distinct()

  fun getOrCreateAppointmentEntity(
    existingAppointment: AppointmentDto,
  ): AppointmentEntity {
    val existing = appointmentEntityRepository.findByDeliusId(existingAppointment.id)
    val name = offenderService.getNameIgnoringLimitedStatus(existingAppointment.offender.crn)
    val projectTypeCode = projectService.getProject(existingAppointment.projectCode)?.projectType?.code
    val projectType = projectTypeCode?.let { projectService.getProjectTypeForCode(it) }

    return if (existing != null) {
      /**
       * Whilst this isn't a water tight approach to keep the date synced with
       * NDelius, it at least ensures the date is correct at the point an appointment
       * is updated from our service (i.e. the outcome is recorded)
       *
       * Ideally we'd instead use domain events or similar to keep this data
       * in sync with NDelius without relying on user interactions to trigger this check
       */
      existing.date = existingAppointment.date

      if (name != null) {
        existing.firstName = name.forename
        existing.lastName = name.surname
      }

      if (projectType != null) {
        existing.projectType = projectType
      }

      appointmentEntityRepository.save(existing)
    } else {
      appointmentEntityRepository.save(existingAppointment.toAppointmentEntity(name?.forename, name?.surname, projectType))
    }
  }

  private companion object {
    const val WITH_OUTCOME = "WITH_OUTCOME"
  }
}

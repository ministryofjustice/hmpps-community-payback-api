package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class ReferenceService(
  val projectTypeEntityRepository: ProjectTypeEntityRepository,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {
  fun getProjectTypes() = projectTypeEntityRepository.findAll().sortedBy { it.name }.toDto()

  fun getContactOutcomes(
    group: ContactOutcomeGroupDto?,
  ): ContactOutcomesDto {
    val outcomes = if (group != null) {
      contactOutcomeEntityRepository.findByGroupsContains(ContactOutcomeGroup.fromDto(group))
    } else {
      contactOutcomeEntityRepository.findAll()
    }

    return outcomes.sortedBy { it.name }.toDto()
  }

  fun getEnforcementActions() = enforcementActionEntityRepository.findAll().sortedBy { it.name }.toDto()
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class ReferenceService(
  val projectTypeEntityRepository: ProjectTypeEntityRepository,
  val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  val communityCampusPduEntityRepository: CommunityCampusPduEntityRepository,
  val enforcementActionEntityRepository: EnforcementActionEntityRepository,
) {
  fun getProjectTypes() = projectTypeEntityRepository.findAllByOrderByNameAsc().toDto()

  fun getContactOutcomes(
    group: ContactOutcomeGroupDto?,
  ) = if (group != null) {
    contactOutcomeEntityRepository.findByGroupsContainsOrderByNameAsc(ContactOutcomeGroup.fromDto(group))
  } else {
    contactOutcomeEntityRepository.findAllByOrderByNameAsc()
  }.toDto()

  fun getEnforcementActions() = enforcementActionEntityRepository.findAllByOrderByNameAsc().toDto()

  fun getCommunityCampusPdus() = communityCampusPduEntityRepository.findAllByOrderByNameAsc().toDto()
}

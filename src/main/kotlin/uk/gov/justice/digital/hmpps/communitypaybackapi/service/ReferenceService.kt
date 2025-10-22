package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
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

  fun getContactOutcomes() = contactOutcomeEntityRepository.findAll().sortedBy { it.name }.toDto()

  fun getEnforcementActions() = enforcementActionEntityRepository.findAll().sortedBy { it.name }.toDto()
}

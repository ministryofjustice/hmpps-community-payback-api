package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntityRepository

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

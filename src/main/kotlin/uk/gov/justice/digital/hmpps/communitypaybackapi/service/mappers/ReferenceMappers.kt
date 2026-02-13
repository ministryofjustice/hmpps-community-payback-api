package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementActionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup

fun List<ProjectTypeEntity>.toDto() = ProjectTypesDto(this.map { it.toDto() })
fun ProjectTypeEntity.toDto() = ProjectTypeDto(
  id = this.id,
  name = this.name,
  code = this.code,
  group = this.projectTypeGroup?.toDto(),
)

fun List<ContactOutcomeEntity>.toDto() = ContactOutcomesDto(this.map { it.toDto() })
fun ContactOutcomeEntity.toDto() = ContactOutcomeDto(
  id = this.id,
  name = this.name,
  code = this.code,
  enforceable = this.enforceable,
  attended = this.attended,
  availableToSupervisors = false,
  willAlertEnforcementDiary = this.enforceable,
)

fun List<EnforcementActionEntity>.toDto() = EnforcementActionsDto(this.map { it.toDto() })
fun EnforcementActionEntity.toDto() = EnforcementActionDto(this.id, this.name, this.code, this.respondByDateRequired)

fun ProjectTypeGroup.toDto() = when (this) {
  ProjectTypeGroup.GROUP -> ProjectTypeGroupDto.GROUP
  ProjectTypeGroup.INDIVIDUAL -> ProjectTypeGroupDto.INDIVIDUAL
  ProjectTypeGroup.INDUCTION -> ProjectTypeGroupDto.INDUCTION
}

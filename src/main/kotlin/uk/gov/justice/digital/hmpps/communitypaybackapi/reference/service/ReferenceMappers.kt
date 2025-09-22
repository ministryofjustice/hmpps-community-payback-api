package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntity

fun ProjectTypes.toDto() = ProjectTypesDto(this.projectTypes.map { it.toDto() })
fun ProjectType.toDto() = ProjectTypeDto(this.id, this.name)

fun List<ContactOutcomeEntity>.toDto() = ContactOutcomesDto(this.map { it.toDto() })
fun ContactOutcomeEntity.toDto() = ContactOutcomeDto(this.id, this.name, this.code)

fun List<EnforcementActionEntity>.toDto() = EnforcementActionsDto(this.map { it.toDto() })
fun EnforcementActionEntity.toDto() = EnforcementActionDto(this.id, this.name, this.code)

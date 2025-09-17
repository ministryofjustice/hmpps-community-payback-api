package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ContactOutcomes
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypesDto

fun ProjectTypes.toDto() = ProjectTypesDto(this.projectTypes.map { it.toDto() })
fun ProjectType.toDto() = ProjectTypeDto(this.id, this.name)
fun ContactOutcomes.toDto() = ContactOutcomesDto(this.contactOutcomes.map { it.toDto() })
fun ContactOutcome.toDto() = ContactOutcomeDto(this.id, this.name)

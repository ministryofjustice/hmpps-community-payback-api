package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.controller.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.controller.ProjectTypesDto

fun ProjectTypes.toDto() = ProjectTypesDto(this.projectTypes.map { it.toDto() })
fun ProjectType.toDto() = ProjectTypeDto(this.id, this.name)

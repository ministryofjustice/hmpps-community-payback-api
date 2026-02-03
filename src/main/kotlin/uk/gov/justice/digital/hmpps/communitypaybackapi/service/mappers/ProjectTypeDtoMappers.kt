package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto

fun ProjectTypeGroupDto.toNDProjectTypeCodes() = when (this) {
  ProjectTypeGroupDto.GROUP -> listOf("PL", "NP1", "NP2")
  ProjectTypeGroupDto.INDIVIDUAL -> listOf("ES", "ICP", "PIP2", "PSP")
}

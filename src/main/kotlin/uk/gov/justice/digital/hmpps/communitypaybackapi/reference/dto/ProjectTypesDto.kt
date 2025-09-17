package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProjectTypeDto(
  @param:Schema(description = "Project type identifier", example = "1234")
  val id: Long,
  @param:Schema(description = "Project type name", example = "Community Garden Maintenance")
  val name: String,
)

data class ProjectTypesDto(
  @param:Schema(description = "List of project types")
  val projectTypes: List<ProjectTypeDto>,
)

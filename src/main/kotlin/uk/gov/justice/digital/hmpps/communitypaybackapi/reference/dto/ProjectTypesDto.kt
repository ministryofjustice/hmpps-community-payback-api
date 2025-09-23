package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ProjectTypeDto(
  @param:Schema(description = "Project type identifier", example = "31656d69-6952-4a2f-85ca-f76f52a8280c")
  val id: UUID,
  @param:Schema(description = "Project type name", example = "Group Placement - Regional Project")
  val name: String,
  @param:Schema(description = "Project type code", example = "NP1")
  val code: String,
)

data class ProjectTypesDto(
  @param:Schema(description = "List of project types")
  val projectTypes: List<ProjectTypeDto>,
)

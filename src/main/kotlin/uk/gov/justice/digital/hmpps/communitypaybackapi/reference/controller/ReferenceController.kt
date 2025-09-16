package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service.ReferenceService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

data class ProjectTypesDto(
  @param:Schema(description = "List of project types")
  val projectTypes: List<ProjectTypeDto>,
)

data class ProjectTypeDto(
  @param:Schema(description = "Project type identifier", example = "1234")
  val id: Long,
  @param:Schema(description = "Project type name", example = "Community Garden Maintenance")
  val name: String,
)

@CommunityPaybackController
@RequestMapping("/references")
class ReferenceController(val referenceService: ReferenceService) {

  @GetMapping("/project-types")
  @Operation(
    description = "Get all project types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project types response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ProjectTypesDto::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getProjectTypes(): ProjectTypesDto = referenceService.getProjectTypes()
}

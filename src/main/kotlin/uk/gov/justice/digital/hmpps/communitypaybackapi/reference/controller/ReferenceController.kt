package uk.gov.justice.digital.hmpps.communitypaybackapi.reference.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.service.ReferenceService

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
    ],
  )
  fun getProjectTypes(): ProjectTypesDto = referenceService.getProjectTypes()

  @GetMapping("/contact-outcomes")
  @Operation(
    description = "Get all contact outcomes",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful contact outcomes response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactOutcomesDto::class),
          ),
        ],
      ),
    ],
  )
  fun getContactOutcomes(): ContactOutcomesDto = referenceService.getContactOutcomes()

  @GetMapping("/enforcement-actions")
  @Operation(
    description = "Get all enforcement actions",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactOutcomesDto::class),
          ),
        ],
      ),
    ],
  )
  fun getEnforcementActions(): EnforcementActionsDto = referenceService.getEnforcementActions()
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ReferenceService

@CommonController
class ReferenceController(val referenceService: ReferenceService) {

  @GetMapping("/references/project-types", "/common/references/project-types")
  @Operation(
    description = "Get all project types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project types response",
      ),
    ],
  )
  fun getProjectTypes(): ProjectTypesDto = referenceService.getProjectTypes()

  @GetMapping("/references/contact-outcomes", "/common/references/contact-outcomes")
  @Operation(
    description = "Get all contact outcomes",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful contact outcomes response",
      ),
    ],
  )
  fun getContactOutcomes(): ContactOutcomesDto = referenceService.getContactOutcomes()

  @GetMapping("/references/enforcement-actions", "/common/references/enforcement-actions")
  @Operation(description = "Get all enforcement actions")
  fun getEnforcementActions(): EnforcementActionsDto = referenceService.getEnforcementActions()
}

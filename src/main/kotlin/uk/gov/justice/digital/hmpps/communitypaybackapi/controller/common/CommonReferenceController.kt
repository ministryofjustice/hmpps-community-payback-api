package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.common

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ContactOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EnforcementActionsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ReferenceService

@CommonController
@RequestMapping(
  "/common/references",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class CommonReferenceController(val referenceService: ReferenceService) {

  @GetMapping("/project-types")
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

  @GetMapping("/contact-outcomes")
  @Operation(
    description = "Get all contact outcomes",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful contact outcomes response",
      ),
    ],
  )
  fun getContactOutcomes(
    @RequestParam
    group: ContactOutcomeGroupDto?,
  ): ContactOutcomesDto = referenceService.getContactOutcomes(group)

  @GetMapping("/enforcement-actions")
  @Operation(description = "Get all enforcement actions")
  fun getEnforcementActions(): EnforcementActionsDto = referenceService.getEnforcementActions()
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@AdminUiController
@RequestMapping(
  "/admin/projects",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminProjectController {

  @GetMapping(
    path = [ "/{projectCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get project for a project code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project response",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @Suppress("UnusedParameter")
  fun getProject(@PathVariable projectCode: String): ProjectDto = throw ResponseStatusException(NOT_IMPLEMENTED, "Not Implemented")
}

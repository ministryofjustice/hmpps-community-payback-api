package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SupervisorService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@SupervisorUiController
@RequestMapping(
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class SupervisorInfoController(
  val supervisorService: SupervisorService,
) {

  @GetMapping(
    path = [ "/supervisor/supervisors"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get supervisor information",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "A supervisor can't be found for the given username",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getSupervisorInfo(
    @RequestParam username: String,
  ) = supervisorService.getSupervisorInfo(username)
}

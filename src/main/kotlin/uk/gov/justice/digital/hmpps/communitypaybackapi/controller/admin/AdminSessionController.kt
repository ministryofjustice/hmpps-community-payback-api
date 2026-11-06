package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AssignSupervisorDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.SessionService
import java.time.LocalDate

@AdminUiController
@RequestMapping(
  "/admin/projects",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminSessionController(val sessionService: SessionService) {

  @PostMapping(
    path = ["/{projectCode}/sessions/{date}/supervisor", "/{projectCode}/supervisor"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Assign a supervisor to a session, or to all sessions for a project when no date is supplied""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Supervisor has been assigned",
      ),
    ],
  )
  fun assignSupervisor(
    @PathVariable projectCode: String,
    @Parameter(description = "Date", example = "2025-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PathVariable(required = false) date: LocalDate?,
    @RequestBody assignment: AssignSupervisorDto,
  ) {
    sessionService.assignSupervisor(
      projectCode = projectCode,
      day = date,
      supervisorCode = assignment.supervisorCode,
    )
  }

  @DeleteMapping(
    path = ["/{projectCode}/sessions/{date}/supervisor/{supervisorCode}", "/{projectCode}/supervisor/{supervisorCode}"],
  )
  @Operation(
    description = """Remove a supervisor assignment for a session or project""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Supervisor assignment has been removed",
      ),
    ],
  )
  fun unassignSupervisor(
    @PathVariable projectCode: String,
    @PathVariable supervisorCode: String,
    @Parameter(description = "Date", example = "2025-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PathVariable(required = false) date: LocalDate?,
  ) {
    sessionService.unassignSupervisor(
      projectCode = projectCode,
      day = date,
      supervisorCode = supervisorCode,
    )
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateEteUserRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EteService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.UUID

@AdminUiController
@RequestMapping(
  "/admin",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminCourseCompletionController(val eteService: EteService) {

  @GetMapping("/providers/{providerCode}/course-completions", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    description = "Get course completions within a date range for a specific provider",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful course completions response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Provider not found",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PageableAsQueryParam
  fun getCourseCompletions(
    @Parameter(
      hidden = true,
      description = "Pagination and sorting parameters. Supported sort fields: courseName, firstName, lastName, completionDate - Default sort: firstname, lastname DESC, size: 50",
      schema = Schema(
        implementation = Pageable::class,
        description = "courseName, firstName, lastName, completionDate are supported for sorting",
      ),
    )
    @PageableDefault(size = 50, sort = ["firstName", "lastName"], direction = Sort.Direction.DESC) pageable: Pageable,
    @PathVariable providerCode: String,
    @RequestParam
    @Parameter(description = "From date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
    @RequestParam
    @Parameter(description = "To date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
  ): Page<EteCourseCompletionEventDto> = eteService.getEteCourseCompletionEvents(
    providerCode,
    dateFrom,
    dateTo,
    pageable,
  )

  @GetMapping("/course-completions/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    description = "Get course completion with an id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful course completion response",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Course completion not found",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourseCompletion(
    @PathVariable id: UUID,
  ): EteCourseCompletionEventDto = eteService.getCourseCompletionEvent(id)

  @PostMapping("/ete/users", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    description = "Create a new ETE user record. Returns 201 if created, 204 if it already exists.",
    responses = [
      ApiResponse(responseCode = "201", description = "User created successfully"),
      ApiResponse(responseCode = "204", description = "User already exists, no action taken"),
    ],
  )
  fun postEteUser(
    @RequestBody @Valid request: CreateEteUserRequest,
  ): ResponseEntity<Unit> {
    val created = eteService.createUser(request.crn, request.email)

    return if (created) {
      ResponseEntity.status(HttpStatus.CREATED).build()
    } else {
      ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
  }
}

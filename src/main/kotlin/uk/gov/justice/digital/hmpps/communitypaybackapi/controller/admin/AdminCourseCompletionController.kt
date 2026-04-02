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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atFirstSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.atLastSecondOfDay
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionRecommendationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionResolutionStatusDto
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
    description = "Get course completions for a specific provider (region) where the course status is 'Passed'",
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
      description = "Pagination and sorting parameters. Supported sort fields: courseName, firstName, lastName, completionDateTime - Default sort: firstname, lastname DESC, size: 50",
      schema = Schema(
        implementation = Pageable::class,
        description = "courseName, firstName, lastName, completionDateTime are supported for sorting",
      ),
    )
    @PageableDefault(size = 50, sort = ["firstName", "lastName"], direction = Sort.Direction.DESC) pageable: Pageable,
    @PathVariable providerCode: String,
    @RequestParam
    pduId: UUID?,
    @RequestParam(required = false)
    @Parameter(description = "Filter by one or more office codes. Example: ?office=London&office=Norwich", example = "London")
    office: List<String>?,
    @RequestParam
    @Parameter(description = "If not defined both resolved and unresolved completions will be returned")
    resolutionStatus: EteCourseCompletionResolutionStatusDto?,
    @RequestParam
    @Parameter(description = "From date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
    @RequestParam
    @Parameter(description = "To date, inclusive", example = "2025-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
  ): Page<EteCourseCompletionEventDto> = eteService.getPassedCourseCompletionEvents(
    providerCode,
    pduId,
    office,
    resolutionStatus = resolutionStatus,
    dateFrom?.atFirstSecondOfDay(),
    dateTo?.atLastSecondOfDay(),
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
  ): EteCourseCompletionEventDto = eteService.getCourseCompletionEvent(id) ?: notFound("Course completion event", id.toString())

  @PostMapping("/course-completions/{id}/resolution", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    description = "Records a resolution for the course completion, potentially creating or updating an appointment.",
    responses = [
      ApiResponse(responseCode = "204", description = "Processed"),
      ApiResponse(responseCode = "404", description = "Course completion not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun postCourseCompletionResolution(
    @PathVariable id: UUID,
    @Valid @RequestBody courseCompletionResolution: CourseCompletionResolutionDto,
  ): ResponseEntity<Unit> {
    ensureCourseCompletionExists(id)
    eteService.recordCourseCompletionResolution(id, courseCompletionResolution)
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
  }

  @GetMapping("/course-completions/{id}/recommended-selection", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    description = "Get recommendations for course completion detail based on the course completion id.",
    responses = [
      ApiResponse(responseCode = "200", description = "Recommendations for CRN, Project Code and UPW Team."),
      ApiResponse(responseCode = "404", description = "Course completion not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  fun getCourseCompletionRecommendation(
    @PathVariable id: UUID,
  ): CourseCompletionRecommendationDto {
    ensureCourseCompletionExists(id)
    return eteService.getCourseCompletionRecommendation(id) ?: notFound("Course completion event", id.toString())
  }

  private fun ensureCourseCompletionExists(id: UUID) = eteService.getCourseCompletionEvent(id) ?: notFound("Course completion event", id.toString())
}

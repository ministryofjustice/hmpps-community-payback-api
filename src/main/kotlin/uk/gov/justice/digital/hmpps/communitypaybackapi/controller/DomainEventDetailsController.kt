package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.OpenApiConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/domain-event-details"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@PreAuthorize("hasRole('" + SecurityConfiguration.ROLE_DOMAIN_EVENT_DETAILS + "')")
@SecurityRequirement(name = OpenApiConfiguration.SECURITY_SCHEME_DOMAIN_EVENT_DETAILS)
class DomainEventDetailsController(
  val appointmentService: AppointmentService,
) {
  @GetMapping(
    path = ["/appointment-outcome/{eventId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "domain event details for a domain event of type 'community-payback.appointment.outcome'",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Appointment outcome details are not available for the given ID",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun appointmentOutcome(@PathVariable eventId: UUID): ResponseEntity<AppointmentOutcomeDomainEventDetailDto> = appointmentService.getOutcomeDomainEventDetails(eventId)?.let {
    ResponseEntity.ok(it)
  } ?: ResponseEntity.notFound().build()
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.domainevent

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.OpenApiConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AppointmentCreatedDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AppointmentUpdatedDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentCreationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentUpdateService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/domain-event-details"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@PreAuthorize("hasRole('" + SecurityConfiguration.ROLE_DOMAIN_EVENT_DETAILS + "')")
@SecurityRequirement(name = OpenApiConfiguration.SECURITY_SCHEME_DOMAIN_EVENT_DETAILS)
@Tag(name = "domain-event-details")
class DomainEventDetailsController(
  val appointmentCreationService: AppointmentCreationService,
  val appointmentUpdateService: AppointmentUpdateService,
) {
  @GetMapping(
    path = ["/appointment-created/{eventId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "domain event details",
      ),
      ApiResponse(
        responseCode = "404",
        description = "A Domain Event does not exist for the given ID",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun appointmentCreated(@PathVariable eventId: UUID): ResponseEntity<AppointmentCreatedDomainEventDetailDto> = appointmentCreationService.getDomainEventDetails(eventId)?.let {
    ResponseEntity.ok(it)
  } ?: ResponseEntity.notFound().build()

  @GetMapping(
    path = ["/appointment-updated/{eventId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "domain event details",
      ),
      ApiResponse(
        responseCode = "404",
        description = "A Domain Event does not exist for the given ID",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun appointmentUpdated(@PathVariable eventId: UUID): ResponseEntity<AppointmentUpdatedDomainEventDetailDto> = appointmentUpdateService.getDomainEventDetails(eventId)?.let {
    ResponseEntity.ok(it)
  } ?: ResponseEntity.notFound().build()
}

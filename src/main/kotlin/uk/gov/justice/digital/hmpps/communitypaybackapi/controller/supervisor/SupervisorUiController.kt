package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.supervisor

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.OpenApiConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@RequestMapping(
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@PreAuthorize("hasRole('" + SecurityConfiguration.ROLE_SUPERVISOR_UI + "')")
@SecurityRequirement(name = OpenApiConfiguration.Companion.SECURITY_SCHEME_SUPERVISOR_UI)
@Tag(name = "supervisor-ui")
internal annotation class SupervisorUiController

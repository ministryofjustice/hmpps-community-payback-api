package uk.gov.justice.digital.hmpps.communitypaybackapi.controller

import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
@PreAuthorize("hasRole('" + SecurityConfiguration.Companion.ROLE_UI + "')")
@SecurityRequirement(name = OpenApiConfiguration.Companion.SECURITY_SCHEME_UI)
internal annotation class CommunityPaybackController

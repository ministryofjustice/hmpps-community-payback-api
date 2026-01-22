package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.OpenApiConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@PreAuthorize("hasRole('" + SecurityConfiguration.ROLE_ADMIN_UI + "')")
@SecurityRequirement(name = OpenApiConfiguration.SECURITY_SCHEME_ADMIN_UI)
@Tag(name = "admin-ui")
internal annotation class AdminUiController

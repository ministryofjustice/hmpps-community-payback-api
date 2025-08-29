package uk.gov.justice.digital.hmpps.communitypaybackapi.example

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@RequestMapping(
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@PreAuthorize("hasRole('ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI')")
@Parameter(name = "X-COMMUNITY-PAYBACK-USERNAME", required = true, allowEmptyValue = false)
internal annotation class CommunityPaybackController

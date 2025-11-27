package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.testsupport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.FormService

@RestController
@RequestMapping(
  path = ["/test-support"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(name = "test-support", description = "Test support endpoints are only available in test environments")
@ConditionalOnProperty(name = ["community-payback.test-support.enabled"], havingValue = "true")
class TestSupportController(
  val formService: FormService,
  val environmentService: EnvironmentService,
) {

  @PostConstruct
  fun onStartup() {
    environmentService.ensureTestEnvironment("test support should not be enabled outside of test environments")
  }

  @DeleteMapping(
    path = ["/forms"],
  )
  @Operation(
    description = """Delete all form data""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Form data deleted",
      ),
    ],
  )
  fun deleteAllForms() {
    ensureLocalRequest()
    formService.deleteAll()
  }

  fun ensureLocalRequest() {
    val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
    val remoteAddress = request.remoteAddr

    if (!listOf("127.0.0.1", "localhost", "0:0:0:0:0:0:0:1").contains(remoteAddress)) {
      error("This endpoint can only be called locally, was requested from: $remoteAddress")
    }
  }
}

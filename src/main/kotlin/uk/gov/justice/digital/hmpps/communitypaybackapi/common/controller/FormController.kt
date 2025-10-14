package uk.gov.justice.digital.hmpps.communitypaybackapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.CommunityPaybackController
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.FormService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@CommunityPaybackController
class FormController(
  private val formService: FormService,
) {

  @GetMapping(
    path = ["/forms/{formType}/{id}"],
  )
  @Operation(
    description = """Fetches the blob stored against this type and id""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "JSON Blob that was previously stored",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No data found for the given type and id",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun formGet(
    @PathVariable formType: String,
    @PathVariable id: String,
  ): String = formService.formGet(formType, id)

  @PutMapping(
    path = ["/forms/{formType}/{id}"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = """Store the JSON blob provided as the request body using the type and id in the request path""",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "JSON blob that will be stored",
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "JSON Blob was stored",
      ),
    ],
  )
  fun formPut(
    @PathVariable formType: String,
    @PathVariable id: String,
    @RequestBody json: String,
  ) {
    formService.formPut(formType, id, json)
  }
}

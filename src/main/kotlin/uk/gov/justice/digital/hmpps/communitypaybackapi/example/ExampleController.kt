package uk.gov.justice.digital.hmpps.communitypaybackapi.example

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ContextService

data class Example(
  @param:Schema(description = "Name of the API", example = "hmpps-community-payback-api")
  @get:JsonProperty("apiName", required = true)
  val apiName: String? = null,
)

@CommunityPaybackController
@RequestMapping("/example")
class ExampleController(
  val contextService: ContextService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @GetMapping(produces = ["application/json"])
  @Operation(
    summary = "Get example API details",
    description = "Returns basic example for the Community Payback API",
    security = [SecurityRequirement(name = "bearerAuth")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful example response",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Example::class),
          ),
        ],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  fun getExample(): Example {
    log.info("Received call from user '${contextService.getUserName()}'")
    return Example(apiName = "hmpps-community-payback-api")
  }

  @PostMapping(consumes = ["application/json"], produces = ["application/json"])
  @Operation(
    summary = "Create an Example",
    description = "Creates a new Example resource",
    security = [SecurityRequirement(name = "bearerAuth")],
    requestBody = RequestBody(
      required = true,
      description = "Example object to create",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = Example::class))],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Created", content = [Content(mediaType = "application/json")]),
      ApiResponse(responseCode = "400", description = "Invalid request"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
    ],
  )
  @ResponseBody
  fun createExample(@org.springframework.web.bind.annotation.RequestBody example: Example): Example = example.copy()

  @PutMapping("/{id}", consumes = ["application/json"], produces = ["application/json"])
  @Operation(
    summary = "Update an Example",
    description = "Updates an existing Example resource",
    security = [SecurityRequirement(name = "bearerAuth")],
    requestBody = RequestBody(
      required = true,
      description = "Updated Example object",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = Example::class))],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Updated successfully", content = [Content(mediaType = "application/json")]),
      ApiResponse(responseCode = "400", description = "Invalid request"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Example not found"),
    ],
  )
  @ResponseBody
  fun updateExample(@PathVariable id: String, @org.springframework.web.bind.annotation.RequestBody example: Example): Example {
    log.info("Example $id updated.")
    return example.copy(apiName = "${example.apiName}-updated")
  }

  @DeleteMapping("/{id}")
  @Operation(
    summary = "Delete an Example",
    description = "Deletes an Example resource by ID",
    security = [SecurityRequirement(name = "bearerAuth")],
    responses = [
      ApiResponse(responseCode = "204", description = "Deleted successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized"),
      ApiResponse(responseCode = "403", description = "Forbidden"),
      ApiResponse(responseCode = "404", description = "Example not found"),
    ],
  )
  fun deleteExample(@PathVariable id: String) {
    log.info("Example $id deleted.")
  }

  @GetMapping("/error")
  @Operation(
    summary = "Raise an error",
    description = "Throws an exception to allow us to test alerting",
    security = [SecurityRequirement(name = "bearerAuth")],
    responses = [
      ApiResponse(responseCode = "500", description = "An error has been raised"),
    ],
  )
  fun error() {
    error("This is an example error to test alerting")
  }
}

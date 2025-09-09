package uk.gov.justice.digital.hmpps.communitypaybackapi.example

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.LoggerFactory
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.DomainEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HmppsDomainEvent

data class Example(
  @param:Schema(description = "Name of the API", example = "hmpps-community-payback-api")
  @get:JsonProperty("apiName", required = true)
  val apiName: String? = null,
)

@CommunityPaybackController
@RequestMapping("/example")
class ExampleController(
  val contextService: ContextService,
  val domainEventPublisher: DomainEventPublisher,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Use spring data paging models
   *
   * pro - works OOTB
   * pro - probation-integration already use this in a few places, straight pass through
   * pro - when using spring data because Page implements Stream<T>, can map values in Page returned from spring data into API type and retain paging metadata
   * pro - very simple to add defaults
   * pro - springdoc automatically documents this (via https://github.com/springdoc/springdoc-openapi/pull/1702)
   * pro - can define application wide defaults via spring.data.web.*
   * con - this adds spring data classes into controller, and means we need spring-data despite not currently using it for persistence
   * con - the sorting definition on the api isn't enumerated. we'd have to add endpoint level documentation for this (TBD, need to look at https://github.com/springdoc/springdoc-openapi/pull/1702)
   * con - the sorting definition on the api may be difficult for UI? 'Sorting criteria in the format: property,(asc|desc)'
   * con - the sorting implies we support multiple sort fields, that may not always be the case
   * con - java not kotlin, so can't use named parameters when building response ourselves
   */
  @GetMapping("searchSpringDataNative", produces = ["application/json"])
  fun getExamples(
    @RequestParam(required = false) name: String,
    @ParameterObject
    @PageableDefault(size = 100, sort = ["name"], direction = Sort.Direction.ASC) pageable: Pageable
  ) = PagedModel<Example>(
    PageImpl(listOf<Example>(),pageable,0)
  )

  /**
   *  ALT approach 1 - custom API types, enumerated sort options, return an item list and include paging metadata in HTTP headers
   *
   *  pro - payload is cleaner (no metadata)
   *  con - less readable in the API definition
   *  con - more difficult to read response logs
   *  con - will need code to convert between this and spring data and/or upstream page models
   */

  /**
   * ALT approach 2 - custom API types, enumerated sort options, return a response containing item list and metadata
   *
   * pro - more readable in the API definition
   * pro - easier to read in response log
   * pro - slightly easier to directly map from spring data Page implementations
   * con - payload is a little more cluttered
   * con - will need code to convert between this and spring data and/or upstream page models
   */

  /**
   * ALT approach 3 - use cursor instead of offset
   *
   * con - relies on upstream services supporting cursor
   */

  @GetMapping(produces = ["application/json"])
  @Operation(
    summary = "Get example API details",
    description = "Returns basic example for the Community Payback API",
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
    description = "Creates a new Example resource and raise a test domain event",
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
  fun createExample(@org.springframework.web.bind.annotation.RequestBody example: Example): Example {
    domainEventPublisher.publish(
      HmppsDomainEvent(
        eventType = "community-payback.test",
        version = 1,
        description = "A test domain event to prove integration",
      ),
    )

    return example.copy()
  }

  @PutMapping("/{id}", consumes = ["application/json"], produces = ["application/json"])
  @Operation(
    summary = "Update an Example",
    description = "Updates an existing Example resource",
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
    responses = [
      ApiResponse(responseCode = "500", description = "An error has been raised"),
    ],
  )
  fun error() {
    error("This is an example error to test alerting")
  }
}

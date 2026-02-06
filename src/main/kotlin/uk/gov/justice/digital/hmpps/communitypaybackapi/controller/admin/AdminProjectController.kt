package uk.gov.justice.digital.hmpps.communitypaybackapi.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BeneficiaryDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.LocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import kotlin.String

@AdminUiController
@RequestMapping(
  "/admin/projects",
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AdminProjectController {

  @GetMapping(
    path = [ "/{projectCode}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    description = "Get project for a project code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successful project response",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @Suppress("UnusedParameter")
  fun getProject(@PathVariable projectCode: String): ProjectDto {
    val location = LocationDto(
      buildingName = null,
      buildingNumber = "1001",
      streetName = "Office Street",
      townCity = "City",
      county = "Shireshire",
      postCode = "ZY98XW",
    )

    return ProjectDto(
      projectName = "Test Project 1",
      projectCode = projectCode,
      location = location,
      hiVisRequired = true,
      beneficiaryDetailsDto = BeneficiaryDetailsDto(
        beneficiary = "McDuck Enterprises",
        contactName = "Scrooge McDuck",
        emailAddress = "scrooge@localhost",
        website = "http://mcduckenterprises.localhost",
        telephoneNumber = "12345 6787890",
        locationDto = location,
      ),
    )
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.mock

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import java.time.LocalDate
import java.time.LocalTime

/**
 * Temporary mock controller until we have the actual endpoint in test environments
 *
 * When removing this also remove the related configuration in [uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration]
 */
@Hidden
@RestController
@RequestMapping(
  value = ["/mocks/community-payback-and-delius"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class MockCommunityPaybackAndDeliusController {

  @GetMapping("/providers")
  fun getProviders() = ProviderSummaries(
    listOf(
      ProviderSummary(id = 1000, name = "East of England"),
      ProviderSummary(id = 2000, name = "North East Region"),
      ProviderSummary(id = 3000, name = "North West Region"),
    ),
  )

  @SuppressWarnings("UnusedParameter")
  @GetMapping("/provider-teams")
  fun getProviderTeams(
    @RequestParam providerId: Long,
  ) = ProviderTeamSummaries(
    listOf(
      ProviderTeamSummary(id = 1001, "Team Lincoln"),
      ProviderTeamSummary(id = 2001, "Team Grantham"),
      ProviderTeamSummary(id = 3001, "Team Boston"),
    ),
  )

  @SuppressWarnings("MagicNumber", "UnusedParameter")
  @GetMapping("/project-allocations")
  fun getProjectAllocations(@RequestParam teamId: Long) = ProjectAllocations(
    listOf(
      ProjectAllocation(
        id = 1L,
        projectName = "Community Garden",
        date = LocalDate.of(2025, 9, 1),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        projectCode = "cg",
        numberOfOffendersAllocated = 40,
        numberOfOffendersWithOutcomes = 0,
        numberOfOffendersWithEA = 0,
      ),
      ProjectAllocation(
        id = 2L,
        projectName = "Park Cleanup",
        date = LocalDate.of(2025, 9, 8),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(16, 0),
        projectCode = "pc",
        numberOfOffendersAllocated = 3,
        numberOfOffendersWithOutcomes = 4,
        numberOfOffendersWithEA = 5,
      ),
    ),
  )

  @GetMapping("/references/project-types")
  fun getReferenceProjectTypes() = ProjectTypes(
    listOf(
      ProjectType(id = 1002, "Community Garden Maintenance"),
      ProjectType(id = 2002, "Park Cleanup"),
      ProjectType(id = 3002, "Library Assistance"),
    ),
  )
}

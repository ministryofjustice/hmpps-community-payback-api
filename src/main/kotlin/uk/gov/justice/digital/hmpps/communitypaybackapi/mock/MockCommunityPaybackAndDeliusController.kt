package uk.gov.justice.digital.hmpps.communitypaybackapi.mock

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
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

  companion object {
    val cases = listOf(
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = "CRN0001",
          name = CaseName("Jack", "Sparrow", middleNames = emptyList()),
          currentExclusion = false,
          currentRestriction = false,
        ),
        isCrnRestricted = { false },
        isCrnExcluded = { false },
      ),
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = "CRN0002",
          name = CaseName("Norman", "Osbourn", middleNames = listOf("Green")),
          currentExclusion = true,
          currentRestriction = false,
        ),
        isCrnRestricted = { false },
        isCrnExcluded = { false },
      ),
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = "CRN0003",
          name = CaseName("Otto", "Octavius", middleNames = listOf("on")),
          currentExclusion = true,
          currentRestriction = false,
        ),
        isCrnRestricted = { it.endsWith("s") },
        isCrnExcluded = { false },
      ),
    )
  }

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

  @PostMapping("/probation-cases/summaries")
  fun getCaseSummaries(
    @Size(min = 1, max = 500, message = "Please provide between 1 and 500 CRNs or NOMIS ids")
    @RequestBody crns: List<String>,
  ): CaseSummaries = CaseSummaries(cases.map { it.caseSummary }.filter { crns.contains(it.crn) })

  @PostMapping("/users/access")
  fun userAccessCheck(
    @RequestParam username: String,
    @Size(min = 1, max = 500, message = "Please provide between 1 and 500 crns") @RequestBody crns: List<String>,
  ) = UserAccess(
    crns.map { crn ->
      cases.firstOrNull { it.caseSummary.crn == crn }?.let {
        CaseAccess(
          crn = crn,
          userRestricted = it.isCrnRestricted(username),
          userExcluded = it.isCrnExcluded(username),
        )
      } ?: CaseAccess(
        crn = crn,
        userRestricted = false,
        userExcluded = false,
      )
    },
  )

  data class CaseSummaryWithRestrictions(
    val caseSummary: CaseSummary,
    val isCrnRestricted: (String) -> Boolean,
    val isCrnExcluded: (String) -> Boolean,
  )
}

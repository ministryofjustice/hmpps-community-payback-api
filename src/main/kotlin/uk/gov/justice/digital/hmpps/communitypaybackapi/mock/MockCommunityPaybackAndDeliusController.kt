package uk.gov.justice.digital.hmpps.communitypaybackapi.mock

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
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
 * and [ResourceSecurityIT]
 */
@Hidden
@RestController
@RequestMapping(
  value = ["/mocks/community-payback-and-delius"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class MockCommunityPaybackAndDeliusController {

  companion object MockCommunityPaybackAndDeliusRepository {

    const val PROJECT1_ID = 101L
    const val PROJECT2_ID = 202L

    const val CRN1 = "CRN0001"
    const val CRN2 = "CRN0002"
    const val CRN3 = "CRN0003"

    const val APPOINTMENT1_ID = 1L
    const val APPOINTMENT2_ID = 2L
    const val APPOINTMENT3_ID = 3L

    val cases = listOf(
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = CRN1,
          name = CaseName("Jack", "Sparrow", middleNames = emptyList()),
          currentExclusion = false,
          currentRestriction = false,
        ),
        isCrnRestricted = { false },
        isCrnExcluded = { false },
      ),
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = CRN2,
          name = CaseName("Norman", "Osbourn", middleNames = listOf("Green")),
          currentExclusion = true,
          currentRestriction = false,
        ),
        isCrnRestricted = { false },
        isCrnExcluded = { false },
      ),
      CaseSummaryWithRestrictions(
        caseSummary = CaseSummary(
          crn = CRN3,
          name = CaseName("Otto", "Octavius", middleNames = listOf("on")),
          currentExclusion = true,
          currentRestriction = false,
        ),
        isCrnRestricted = { it.endsWith("s") },
        isCrnExcluded = { false },
      ),
    )

    val mockProject1 = MockProject(
      id = PROJECT1_ID,
      name = "Community Garden",
      code = "cg",
    )

    val mockProject2 = MockProject(
      id = PROJECT2_ID,
      name = "Park Cleanup",
      code = "pc",
    )

    val mockProjectAllocations = listOf(
      MockProjectAllocation(
        id = 1L,
        project = mockProject1,
        date = LocalDate.of(2025, 9, 1),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        appointments = listOf(
          MockProjectAppointment(
            id = APPOINTMENT1_ID,
            project = mockProject1,
            crn = CRN1,
            requirementMinutes = 600,
            completedMinutes = 60,
          ),
          MockProjectAppointment(
            id = APPOINTMENT2_ID,
            project = mockProject1,
            crn = CRN2,
            requirementMinutes = 300,
            completedMinutes = 30,
          ),
        ),
      ),
      MockProjectAllocation(
        id = 2L,
        project = mockProject2,
        date = LocalDate.of(2025, 9, 8),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(16, 0),
        appointments = listOf(
          MockProjectAppointment(
            id = APPOINTMENT3_ID,
            project = mockProject1,
            crn = CRN1,
            requirementMinutes = 1200,
            completedMinutes = 0,
          ),
        ),
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
    mockProjectAllocations.map {
      ProjectAllocation(
        id = it.id,
        projectId = it.project.id,
        projectName = it.project.name,
        date = it.date,
        startTime = it.startTime,
        endTime = it.endTime,
        projectCode = it.project.code,
        numberOfOffendersAllocated = it.appointments.size,
        numberOfOffendersWithOutcomes = 0,
        numberOfOffendersWithEA = 0,
      )
    },
  )

  @GetMapping("/appointments/{appointmentId}")
  fun getProjectAppointment(@PathVariable appointmentId: Long): ResponseEntity<ProjectAppointment> = mockProjectAllocations
    .flatMap { it.appointments }
    .firstOrNull { it.id == appointmentId }
    ?.let { ResponseEntity.ok(it.toProjectAppointment()) }
    ?: ResponseEntity.notFound().build()

  @GetMapping("/projects/{projectId}/appointments")
  fun getProjectAppointments(
    @PathVariable projectId: Long,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ): ProjectAppointments {
    val matchingAllocation = mockProjectAllocations
      .firstOrNull { it.project.id == projectId && it.date == date }

    if (matchingAllocation == null) {
      return ProjectAppointments(emptyList())
    }

    return ProjectAppointments(matchingAllocation.appointments.map { it.toProjectAppointment() })
  }

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

  data class MockProject(
    val id: Long,
    val name: String,
    val code: String,
  )

  data class MockProjectAllocation(
    val id: Long,
    val project: MockProject,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val appointments: List<MockProjectAppointment>,
  )

  data class MockProjectAppointment(
    val id: Long,
    val project: MockProject,
    val crn: String,
    val requirementMinutes: Int,
    val completedMinutes: Int,
  ) {
    fun toProjectAppointment() = ProjectAppointment(
      id = this.id,
      projectName = this.project.name,
      projectCode = this.project.code,
      crn = this.crn,
      requirementMinutes = this.requirementMinutes,
      completedMinutes = this.completedMinutes,
    )
  }
}

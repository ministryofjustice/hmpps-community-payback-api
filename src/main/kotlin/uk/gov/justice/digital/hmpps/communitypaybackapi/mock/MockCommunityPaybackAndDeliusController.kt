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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusRepository.cases
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusRepository.mockProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.mock.MockCommunityPaybackAndDeliusRepository.mockProjectSessions
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

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
    mockProjectSessions.map {
      ProjectAllocation(
        id = Random.nextLong(),
        projectId = it.project.id,
        projectName = it.project.name,
        date = it.date,
        startTime = it.startTime,
        endTime = it.endTime,
        projectCode = it.project.code,
        numberOfOffendersAllocated = it.appointmentSummaries.size,
        numberOfOffendersWithOutcomes = 0,
        numberOfOffendersWithEA = 0,
      )
    },
  )

  @GetMapping("/appointments/{appointmentId}")
  fun getProjectAppointment(@PathVariable appointmentId: Long): ResponseEntity<ProjectAppointment> = mockProjectAppointments
    .find { it.id == appointmentId }
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()

  @GetMapping("/{projectCode}/sessions/{date}")
  fun getSession(
    @PathVariable projectCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ): ProjectSession = mockProjectSessions.find { it.project.code == projectCode && it.date == date }?.toProjectSession()
    ?: throw IllegalArgumentException("Session not found")

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
    val location: String,
  )

  data class MockProjectSession(
    val project: MockProject,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val appointmentSummaries: List<MockProjectAppointmentSummary>,
  ) {
    fun toProjectSession() = ProjectSession(
      projectName = project.name,
      projectCode = project.code,
      projectLocation = project.location,
      endTime = endTime,
      startTime = startTime,
      date = date,
      appointmentSummaries = appointmentSummaries.map { it.toProjectAppointment() },
    )
  }

  data class MockProjectAppointmentSummary(
    val id: Long,
    val crn: String,
    val requirementMinutes: Int,
    val completedMinutes: Int,
  ) {
    fun toProjectAppointment() = ProjectAppointmentSummary(
      id = this.id,
      crn = this.crn,
      requirementMinutes = this.requirementMinutes,
      completedMinutes = this.completedMinutes,
    )
  }
}

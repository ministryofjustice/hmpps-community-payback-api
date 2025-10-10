package uk.gov.justice.digital.hmpps.communitypaybackapi.common.client

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun providers(): ProviderSummaries

  @GetExchange("/teams")
  fun providerTeams(@RequestParam providerCode: String): ProviderTeamSummaries

  @GetExchange("/projects/session-search")
  fun getProjectSessions(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam teamCode: String,
  ): ProjectSessionSummaries

  @GetExchange("/appointments/{appointmentId}")
  fun getProjectAppointment(
    @PathVariable appointmentId: Long,
  ): ProjectAppointment

  @GetExchange("/projects/{projectCode}/sessions/{date}")
  fun getProjectSession(
    @PathVariable projectCode: String,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestParam("startTime") @DateTimeFormat(pattern = "HH:mm") start: LocalTime,
    @RequestParam("endTime") @DateTimeFormat(pattern = "HH:mm") end: LocalTime,
  ): ProjectSession

  @PostExchange("/probation-cases/summaries")
  fun getCaseSummaries(
    @RequestBody crns: Set<String>,
  ): CaseSummaries

  @PostExchange("/users/access")
  fun getUsersAccess(
    @RequestParam username: String,
    @RequestBody crns: Set<String>,
  ): UserAccess

  @GetExchange("/supervisors")
  fun teamSupervisors(
    @RequestParam providerCode: String,
    @RequestParam teamCode: String,
  ): SupervisorSummaries
}

data class ProviderSummaries(
  val providers: List<ProviderSummary>,
)

data class ProviderSummary(
  val id: Long,
  val code: String,
  val name: String,
)

data class ProviderTeamSummaries(
  val teams: List<ProviderTeamSummary>,
)

data class ProviderTeamSummary(
  val id: Long,
  val code: String,
  val description: String,
)
data class ProjectSessionSummaries(
  val sessions: List<ProjectSummary>,
)

data class ProjectSummary(
  val id: Long,
  val projectId: Long,
  val date: LocalDate,
  val projectName: String,
  val projectCode: String,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val allocatedCount: Int,
  val compliedOutcomeCount: Int,
  val enforcementActionNeededCount: Int,
)

data class ProjectSession(
  val projectName: String,
  val projectCode: String,
  val projectLocation: String,
  val sessionStartTime: LocalTime,
  val sessionEndTime: LocalTime,
  val date: LocalDate,
  val appointmentSummaries: List<ProjectAppointmentSummary>,
) {
  companion object
}

data class ProjectAppointmentSummary(
  val appointmentId: Long,
  val crn: String,
  val requirementMinutes: Int,
  val completedMinutes: Int,
) {
  companion object
}

data class ProjectAppointment(
  val id: Long,
  val projectName: String,
  val projectCode: String,
  val projectTypeName: String,
  val projectTypeCode: String,
  val crn: String,
  val supervisingTeam: String,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val penaltyMinutes: Long?,
  val supervisorCode: String?,
  val contactOutcomeId: UUID?,
  val enforcementActionId: UUID?,
  val respondBy: LocalDate?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val workQuality: ProjectAppointmentWorkQuality?,
  val behaviour: ProjectAppointmentBehaviour?,
  val notes: String?,
) {
  companion object
}

enum class ProjectAppointmentWorkQuality {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

enum class ProjectAppointmentBehaviour {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

data class CaseSummaries(
  var cases: List<CaseSummary>,
)

data class CaseSummary(
  val crn: String,
  val name: CaseName,
  val currentExclusion: Boolean = false,
  val currentRestriction: Boolean = false,
) {
  companion object
}

data class CaseName(
  val forename: String,
  val surname: String,
  val middleNames: List<String> = emptyList(),
)

data class CaseAccess(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
)

data class UserAccess(val access: List<CaseAccess>)

data class SupervisorSummaries(
  val supervisors: List<SupervisorSummary>,
)

data class SupervisorSummary(
  val id: Long,
  val surname: String,
  val forename: String,
  val forename2: String?,
  val officerCode: String,
  val staffGrade: String,
)

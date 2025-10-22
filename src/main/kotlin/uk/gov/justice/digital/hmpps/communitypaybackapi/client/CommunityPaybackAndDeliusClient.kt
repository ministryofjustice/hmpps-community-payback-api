package uk.gov.justice.digital.hmpps.communitypaybackapi.client

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
  fun getProviders(): ProviderSummaries

  @GetExchange("/providers/{providerCode}/teams")
  fun getProviderTeams(@PathVariable providerCode: String): ProviderTeamSummaries

  @GetExchange("/sessions")
  fun getSessions(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam teamCode: String,
  ): ProjectSessionSummaries

  @GetExchange("/projects/{projectCode}/sessions/appointments")
  fun getProjectSession(
    @PathVariable projectCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestParam @DateTimeFormat(pattern = "HH:mm") startTime: LocalTime,
    @RequestParam @DateTimeFormat(pattern = "HH:mm") endTime: LocalTime,
  ): ProjectSession

  @GetExchange("/appointments/{appointmentId}")
  fun getProjectAppointment(
    @PathVariable appointmentId: Long,
  ): ProjectAppointment

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
  val code: String,
  val name: String,
)

data class ProviderTeamSummaries(
  val teams: List<ProviderTeamSummary>,
)

data class ProviderTeamSummary(
  val code: String,
  val description: String,
)
data class ProjectSessionSummaries(
  val sessions: List<ProjectSessionSummary>,
)

data class ProjectSessionSummary(
  val date: LocalDate,
  val project: ProjectSummary,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val allocatedCount: Int,
  val compliedOutcomeCount: Int,
  val enforcementActionNeededCount: Int,
) {
  companion object
}

data class ProjectSession(
  val project: Project,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val date: LocalDate,
  val appointmentSummaries: List<ProjectAppointmentSummary>,
) {
  companion object
}

data class ProjectAppointmentSummary(
  val id: Long,
  val case: CaseSummary,
  val requirementProgress: RequirementProgress,
) {
  companion object
}

data class RequirementProgress(
  val requirementMinutes: Int,
  val completedMinutes: Int,
) {
  companion object
}

data class ProjectAppointment(
  val id: Long,
  val project: Project,
  val projectType: ProjectType,
  val case: CaseSummary,
  val team: Team,
  val provider: Provider,
  val pickUpData: PickUpData?,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val penaltyTime: LocalTime?,
  val supervisorOfficerCode: String?,
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

data class Project(val name: String, val code: String, val location: ProjectLocation) {
  companion object
}
data class ProjectSummary(val name: String, val code: String) {
  companion object
}
data class ProjectType(val name: String, val code: String) {
  companion object
}
data class Team(val name: String, val code: String) {
  companion object
}
data class Provider(val name: String, val code: String) {
  companion object
}

data class PickUpData(
  val pickUpLocation: PickUpLocation?,
  val time: LocalTime?,
) {
  companion object
}

data class PickUpLocation(
  val buildingName: String? = null,
  val buildingNumber: String? = null,
  val streetName: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val postCode: String? = null,
) {
  companion object
}

data class ProjectLocation(
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val postCode: String? = null,
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
  val surname: String,
  val forename: String,
  val forename2: String?,
  val officerCode: String,
  val staffGrade: String,
)

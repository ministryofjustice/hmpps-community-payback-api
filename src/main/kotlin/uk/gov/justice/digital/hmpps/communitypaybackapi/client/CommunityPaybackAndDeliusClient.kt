package uk.gov.justice.digital.hmpps.communitypaybackapi.client

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

interface CommunityPaybackAndDeliusClient {
  @GetExchange("/providers")
  fun getProviders(
    @RequestParam username: String,
  ): ProviderSummaries

  @GetExchange("/providers/{providerCode}/teams")
  fun getProviderTeams(@PathVariable providerCode: String): ProviderTeamSummaries

  @GetExchange("/sessions")
  fun getSessions(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam teamCode: String,
  ): SessionSummaries

  @GetExchange("/projects/{projectCode}/sessions/appointments")
  fun getSession(
    @PathVariable projectCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestParam @DateTimeFormat(pattern = "HH:mm") startTime: LocalTime,
    @RequestParam @DateTimeFormat(pattern = "HH:mm") endTime: LocalTime,
  ): Session

  @GetExchange("/appointments/{appointmentId}")
  fun getProjectAppointment(
    @PathVariable appointmentId: Long,
  ): ProjectAppointment

  @PutExchange("/appointments/{appointmentId}")
  fun updateAppointment(
    @PathVariable appointmentId: Long,
    @RequestBody updateAppointment: UpdateAppointment,
  )

  @PostExchange("/users/access")
  fun getUsersAccess(
    @RequestParam username: String,
    @RequestBody crns: Set<String>,
  ): UserAccess

  @GetExchange("/providers/{providerCode}/teams/{teamCode}/supervisors")
  fun teamSupervisors(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
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
data class SessionSummaries(
  val sessions: List<SessionSummary>,
)

data class SessionSummary(
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

data class Session(
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
  val outcome: ContactOutcome?,
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
  val version: UUID,
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
  val supervisor: AppointmentSupervisor?,
  val outcome: ContactOutcome?,
  val enforcementAction: EnforcementAction?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val workQuality: ProjectAppointmentWorkQuality?,
  val behaviour: ProjectAppointmentBehaviour?,
  val notes: String?,
  val sensitive: Boolean?,
  val alertActive: Boolean?,
) {
  companion object
}

data class AppointmentSupervisor(val code: String, val name: Name) {
  companion object
}
data class ContactOutcome(val code: String, val description: String) {
  companion object
}
data class EnforcementAction(val code: String, val description: String, val respondBy: LocalDate?) {
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
  val name: Name,
  val dateOfBirth: LocalDate,
  val currentExclusion: Boolean = false,
  val currentRestriction: Boolean = false,
) {
  companion object
}

data class Name(
  val forename: String,
  val surname: String,
  val middleNames: List<String> = emptyList(),
) {
  companion object
}

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
  val name: SupervisorName,
  val code: String,
  val grade: Grade?,
) {
  companion object
}

data class Grade(
  val code: String,
  val description: String,
)

data class SupervisorName(
  val forename: String,
  val surname: String,
  val middleName: String?,
) {
  companion object
}

data class UpdateAppointment(
  val version: UUID,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "09:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val contactOutcomeCode: String? = null,
  val supervisorOfficerCode: String? = null,
  val notes: String? = null,
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,
  val workQuality: ProjectAppointmentWorkQuality? = null,
  val behaviour: ProjectAppointmentBehaviour? = null,
  val sensitive: Boolean? = null,
  val alertActive: Boolean? = null,
  val enforcementActionCode: String? = null,
  val respondBy: LocalDate? = null,
)

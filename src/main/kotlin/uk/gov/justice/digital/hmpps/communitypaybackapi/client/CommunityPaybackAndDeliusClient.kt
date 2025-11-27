package uk.gov.justice.digital.hmpps.communitypaybackapi.client

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.HourMinuteDuration
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

  @GetExchange("/providers/{providerCode}/teams/{teamCode}/sessions")
  fun getSessions(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
  ): SessionSummaries

  @GetExchange("/projects/{projectCode}/appointments")
  fun getSession(
    @PathVariable projectCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestParam username: String,
  ): Session

  @GetExchange("/supervisors")
  fun getSupervisor(
    @RequestParam username: String,
  ): Supervisor

  @GetExchange("/projects/{projectCode}/appointments/{appointmentId}")
  fun getAppointment(
    @PathVariable projectCode: String,
    @PathVariable appointmentId: Long,
    @RequestParam username: String,
  ): Appointment

  @PutExchange("/projects/{projectCode}/appointments/{appointmentId}/outcome")
  fun updateAppointment(
    @PathVariable projectCode: String,
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
) {
  companion object
}

data class SessionSummary(
  val date: LocalDate,
  val project: ProjectSummary,
  val allocatedCount: Int,
  val outcomeCount: Int,
  val enforcementActionCount: Int,
) {
  companion object
}

data class Session(
  val project: Project,
  val appointmentSummaries: List<AppointmentSummary>,
) {
  companion object
}

data class AppointmentSummary(
  val id: Long,
  val case: CaseSummary,
  val outcome: ContactOutcome?,
  val requirementProgress: RequirementProgress,
) {
  fun hasOutcome() = outcome != null

  companion object
}

data class RequirementProgress(
  val requiredMinutes: Int,
  val completedMinutes: Int,
) {
  companion object
}

data class Appointment(
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
  val penaltyHours: HourMinuteDuration?,
  val supervisor: AppointmentSupervisor,
  val outcome: ContactOutcome?,
  val enforcementAction: EnforcementAction?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val workQuality: AppointmentWorkQuality?,
  val behaviour: AppointmentBehaviour?,
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
data class Project(val name: String, val code: String, val location: Address) {
  companion object
}
data class ProjectSummary(val description: String, val code: String) {
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
  val pickUpLocation: Address?,
  val time: LocalTime?,
) {
  companion object
}

data class Address(
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val postCode: String? = null,
) {
  companion object
}

enum class AppointmentWorkQuality {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

enum class AppointmentBehaviour {
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

data class Supervisor(
  val code: String,
  val isUnpaidWorkTeamMember: Boolean,
) {
  companion object
}

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
  val workQuality: AppointmentWorkQuality? = null,
  val behaviour: AppointmentBehaviour? = null,
  val sensitive: Boolean? = null,
  val alertActive: Boolean? = null,
)

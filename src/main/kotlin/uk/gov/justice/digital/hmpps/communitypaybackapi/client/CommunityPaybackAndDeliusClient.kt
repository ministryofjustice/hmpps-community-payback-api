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
  ): NDProviderSummaries

  @GetExchange("/providers/{providerCode}/teams")
  fun getProviderTeams(@PathVariable providerCode: String): NDProviderTeamSummaries

  @GetExchange("/providers/{providerCode}/teams/{teamCode}/sessions")
  fun getSessions(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    @RequestParam projectTypeCodes: List<String>?,
  ): NDSessionSummaries

  @GetExchange("/projects/{projectCode}")
  fun getProject(
    @PathVariable projectCode: String,
  ): NDProject

  @GetExchange("/projects/{projectCode}/appointments")
  fun getSession(
    @PathVariable projectCode: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestParam username: String,
  ): NDSession

  @GetExchange("/supervisors")
  fun getSupervisor(
    @RequestParam username: String,
  ): NDSupervisor

  @GetExchange("/projects/{projectCode}/appointments/{appointmentId}")
  fun getAppointment(
    @PathVariable projectCode: String,
    @PathVariable appointmentId: Long,
    @RequestParam username: String,
  ): NDAppointment

  @PutExchange("/projects/{projectCode}/appointments/{appointmentId}/outcome")
  fun updateAppointment(
    @PathVariable projectCode: String,
    @PathVariable appointmentId: Long,
    @RequestBody updateAppointment: NDUpdateAppointment,
  )

  @PostExchange("/projects/{projectCode}/appointments")
  fun createAppointments(
    @PathVariable projectCode: String,
    @RequestBody appointments: NDCreateAppointments,
  ): List<NDCreatedAppointment>

  @GetExchange("/providers/{providerCode}/teams/{teamCode}/supervisors")
  fun teamSupervisors(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
  ): NDSupervisorSummaries

  @GetExchange("/case/{crn}/event/{eventNumber}/appointments/schedule")
  fun getUnpaidWorkRequirement(
    @PathVariable crn: String,
    @PathVariable eventNumber: Int,
  ): NDUnpaidWorkRequirement

  @GetExchange("/reference-data/non-working-days")
  fun getNonWorkingDays(): List<LocalDate>

  @GetExchange("/providers/{providerCode}/teams/{teamCode}/projects")
  fun getProjects(
    @PathVariable providerCode: String,
    @PathVariable teamCode: String,
    @RequestParam projectTypeCodes: List<String>?,
    @RequestParam params: Map<String, String>,
  ): PageResponse<NDProjectOutcomeSummary>
}

data class NDProviderSummaries(
  val providers: List<NDProviderSummary>,
)

data class NDProviderSummary(
  val code: String,
  val name: String,
)

data class NDProviderTeamSummaries(
  val teams: List<NDProviderTeamSummary>,
)

data class NDProviderTeamSummary(
  val code: String,
  val description: String,
)

data class NDSessionSummaries(
  val sessions: List<NDSessionSummary>,
) {
  companion object
}

data class NDSessionSummary(
  val date: LocalDate,
  val project: NDProjectSummary,
  val allocatedCount: Int,
  val outcomeCount: Int,
  val enforcementActionCount: Int,
) {
  companion object
}

data class NDSession(
  val project: NDProjectAndLocation,
  val appointmentSummaries: List<NDAppointmentSummary>,
) {
  companion object
}

data class NDAppointmentSummary(
  val id: Long,
  val case: NDCaseSummary,
  val outcome: NDContactOutcome?,
  val requirementProgress: NDRequirementProgress,
) {
  fun hasOutcome() = outcome != null

  companion object
}

data class NDRequirementProgress(
  /**
   * requirement minutes. does not include adjustments
   */
  val requiredMinutes: Int,
  /**
   * minutes credited from completed appointments
   */
  val completedMinutes: Int,
  /**
   * adjustments to the requirement, in minutes. A positive
   * number means 'add more time to the requirement'
   */
  val adjustments: Int,
) {
  companion object
}

data class NDAppointment(
  val id: Long,
  val reference: UUID?,
  val version: UUID,
  val project: NDProjectAndLocation,
  val projectType: NDProjectType,
  val case: NDCaseSummary,
  val event: NDEvent,
  val team: NDTeam,
  val provider: NDProvider,
  val pickUpData: NDAppointmentPickUp?,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val penaltyHours: HourMinuteDuration?,
  val supervisor: NDAppointmentSupervisor,
  val outcome: NDContactOutcome?,
  val enforcementAction: NDEnforcementAction?,
  val hiVisWorn: Boolean?,
  val workedIntensively: Boolean?,
  val workQuality: NDAppointmentWorkQuality?,
  val behaviour: NDAppointmentBehaviour?,
  val notes: String?,
  val sensitive: Boolean?,
  val alertActive: Boolean?,
) {
  companion object
}

data class NDAppointmentSupervisor(val code: String, val name: NDName) {
  companion object
}
data class NDContactOutcome(val code: String, val description: String) {
  companion object
}
data class NDEnforcementAction(val code: String, val description: String, val respondBy: LocalDate?) {
  companion object
}

data class NDProject(
  val name: String,
  val code: String,
  val projectTypeCode: String,
  val location: NDAddress,
  val beneficiaryDetails: NDBeneficiaryDetails,
  val hiVisRequired: Boolean,
) {
  companion object
}

data class NDBeneficiaryDetails(
  val beneficiary: String,
  val contactName: String,
  val emailAddress: String?,
  val website: String?,
  val telephoneNumber: String,
  val location: NDAddress,
) {
  companion object
}

data class NDProjectAndLocation(val name: String, val code: String, val location: NDAddress) {
  companion object
}
data class NDProjectOutcomeSummary(val name: String, val code: String, val location: NDAddress, val overdueOutcomesCount: Int, val oldestOverdueInDays: Int) {
  companion object
}
data class NDProjectSummary(val description: String, val code: String) {
  companion object
}
data class NDProjectType(val name: String, val code: String) {
  companion object
}
data class NDTeam(val name: String, val code: String) {
  companion object
}
data class NDProvider(val name: String, val code: String) {
  companion object
}

data class NDPickUpLocation(
  val code: String,
  val description: String,
  val streetName: String?,
  val buildingName: String?,
  val addressNumber: String?,
  val townCity: String?,
  val county: String?,
  val postCode: String?,
) {
  companion object
}

data class NDAppointmentPickUp(
  val location: NDPickUpLocation?,
  val time: LocalTime?,
) {
  companion object
}

data class NDAddress(
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val postCode: String? = null,
) {
  companion object
}

enum class NDAppointmentWorkQuality {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

enum class NDAppointmentBehaviour {
  EXCELLENT,
  GOOD,
  NOT_APPLICABLE,
  POOR,
  SATISFACTORY,
  UNSATISFACTORY,
}

data class NDCaseSummary(
  val crn: String,
  val name: NDName,
  val dateOfBirth: LocalDate,
  val currentExclusion: Boolean,
  val currentRestriction: Boolean,
) {
  companion object
}

data class NDEvent(
  val number: Int,
) {
  companion object
}

data class NDName(
  val forename: String,
  val surname: String,
  val middleNames: List<String> = emptyList(),
) {
  companion object
}

data class NDSupervisor(
  val code: String,
  val isUnpaidWorkTeamMember: Boolean,
  val unpaidWorkTeams: List<NDSupervisorTeam>,
) {
  companion object
}

data class NDSupervisorTeam(
  val code: String,
  val description: String,
  val provider: NDCodeDescription,
)

data class NDCodeDescription(
  val code: String,
  val description: String,
)

data class NDNameCode(
  val name: String,
  val code: String,
) {
  companion object
}

data class NDSupervisorSummaries(
  val supervisors: List<NDSupervisorSummary>,
)

data class NDSupervisorSummary(
  val name: NDSupervisorName,
  val code: String,
  val grade: NDGrade?,
) {
  companion object
}

data class NDGrade(
  val code: String,
  val description: String,
)

data class NDSupervisorName(
  val forename: String,
  val surname: String,
  val middleName: String?,
) {
  companion object
}

data class NDCreateAppointments(
  val appointments: List<NDCreateAppointment>,
)

data class NDCreateAppointment(
  val reference: UUID,
  val crn: String,
  val eventNumber: Int,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val outcome: NDCode? = null,
  val supervisor: NDCode? = null,
  val notes: String? = null,
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,
  val minutesCredited: Long? = null,
  val workQuality: NDAppointmentWorkQuality? = null,
  val behaviour: NDAppointmentBehaviour? = null,
  val sensitive: Boolean? = null,
  val alertActive: Boolean? = null,
  val allocationId: Long? = null,
  val pickUp: NDPickUp?,
)

data class NDCreatedAppointment(
  val id: Long,
  val reference: UUID,
)

data class NDUpdateAppointment(
  val version: UUID,
  @param:Schema(example = "09:00", description = "The start local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val startTime: LocalTime,
  @param:Schema(example = "09:00", description = "The end local time of the appointment", pattern = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
  val endTime: LocalTime,
  val outcome: NDCode? = null,
  val supervisor: NDCode,
  val notes: String? = null,
  val hiVisWorn: Boolean? = null,
  val workedIntensively: Boolean? = null,
  val penaltyMinutes: Long? = null,
  val minutesCredited: Long? = null,
  val workQuality: NDAppointmentWorkQuality? = null,
  val behaviour: NDAppointmentBehaviour? = null,
  val sensitive: Boolean? = null,
  val alertActive: Boolean? = null,
)

data class NDCode(
  val code: String,
)

data class NDUnpaidWorkRequirement(
  val requirementProgress: NDRequirementProgress,
  val allocations: List<NDSchedulingAllocation>,
  val appointments: List<NDSchedulingExistingAppointment>,
) {
  companion object
}

data class NDSchedulingAllocation(
  val id: Long,
  val project: NDSchedulingProject,
  val projectAvailability: NDSchedulingAvailability?,
  val frequency: NDSchedulingFrequency?,
  val dayOfWeek: NDSchedulingDayOfWeek,
  val startDateInclusive: LocalDate,
  val endDateInclusive: LocalDate?,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val pickUp: NDPickUp?,
) {
  companion object
}

data class NDPickUp(val time: LocalTime?, val location: NDCode?)

data class NDSchedulingProject(
  val name: String,
  val code: String,
  val expectedEndDateExclusive: LocalDate?,
  val actualEndDateExclusive: LocalDate?,
  val type: NDNameCode,
  val provider: NDNameCode,
  val team: NDNameCode,
) {
  companion object
}

data class NDSchedulingAvailability(
  val frequency: NDSchedulingFrequency?,
  val endDateExclusive: LocalDate?,
) {
  companion object
}

enum class NDSchedulingFrequency {
  Once,
  Weekly,
  Fortnightly,
}

enum class NDSchedulingDayOfWeek {
  Monday,
  Tuesday,
  Wednesday,
  Thursday,
  Friday,
  Saturday,
  Sunday,
}

data class NDSchedulingExistingAppointment(
  val id: Long,
  val project: NDNameCode,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val outcome: NDCodeDescription?,
  val minutesCredited: Long?,
  val allocationId: Long?,
) {
  companion object
}

data class PageResponse<T>(
  val content: List<T>,
  val page: PageMeta,
) {
  data class PageMeta(
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int,
  )
}

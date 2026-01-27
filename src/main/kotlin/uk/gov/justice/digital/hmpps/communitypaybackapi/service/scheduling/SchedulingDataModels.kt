package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class SchedulingRequest(
  val id: UUID = UUID.randomUUID(),
  val today: LocalDate,
  val trigger: SchedulingTrigger,
  val requirement: SchedulingRequirement,
  val allocations: SchedulingAllocations,
  val existingAppointments: SchedulingExistingAppointments,
  val nonWorkingDates: SchedulingNonWorkingDates,
  val dryRun: Boolean,
) {
  companion object
}

data class SchedulingTrigger(
  val type: SchedulingTriggerType,
  val description: String,
) {
  companion object
}

enum class SchedulingTriggerType {
  AppointmentChange,
}

data class SchedulingRequirement(
  val crn: String,
  val deliusEventNumber: Int,
  /**
   * The corresponding community payback requirement length, before any
   * time is credited from attended appointments. This will include adjustments
   */
  val requirementLengthMinutes: Duration,
) {
  companion object
}

data class SchedulingProject(
  val code: String,
  val projectTypeCode: String,
  val providerCode: String,
  val teamCode: String,
) {
  companion object
}

enum class SchedulingFrequency {
  /**
   * Only 1 appointment of the selected allocation will be added, for the next available date on or after the start date specified.
   * This allocation is applied every-time scheduling is ran and disregards any past appointments linked to the allocation,
   * meaning there can be multiple appointments linked to this allocation
   */
  ONCE,

  /**
   * 1 appointment per week will be added for as many appointments as are available, starting from the next available date on or
   * after the start date specified.
   */
  WEEKLY,

  /**
   * 1 appointment per fortnight will be added for as many appointments as are available, starting from the next available date on or
   * after the start date specified.
   */
  FORTNIGHTLY,
}

data class SchedulingAllocations(
  val allocations: List<SchedulingAllocation>,
)

/**
 * Derived by combining values from the requirement's Allocation
 * and its associated Availability and Project
 */
data class SchedulingAllocation(
  val id: Long,
  val alias: String?,
  val project: SchedulingProject,
  /**
   * The longest frequency of the allocation and availability, or weekly if neither is defined
   */
  val frequency: SchedulingFrequency,
  val dayOfWeek: DayOfWeek,
  val startDateInclusive: LocalDate,
  /**
   * The case allocation end date (inclusive) or if that isn't set, the earliest of availability end date,
   * project expected end date or project actual end date (if any are defined they are exclusive)
   */
  val endDateInclusive: LocalDate?,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val pickUpLocationCode: String?,
  val pickUpTime: LocalTime?,
) {
  companion object
}

data class SchedulingExistingAppointments(
  val appointments: List<SchedulingExistingAppointment>,
)

data class SchedulingExistingAppointment(
  val id: Long,
  val projectCode: String,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val hasOutcome: Boolean,
  val minutesCredited: Duration?,
  val allocationId: Long?,
) {
  companion object
}

data class SchedulingRequiredAppointment(
  val id: UUID = UUID.randomUUID(),
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val project: SchedulingProject,
  val allocation: SchedulingAllocation,
) {
  companion object
}

data class SchedulingNonWorkingDates(
  val dates: List<LocalDate>,
)

/**
 * A schedule details all appointments required as of today
 * to satisfy remaining community payback requirement minutes
 */
data class Schedule(
  val requiredAppointmentsAsOfToday: List<SchedulingRequiredAppointment>,
  val forcedRetentions: List<SchedulingExistingAppointment>,
  /**
   * The remaining minutes we were unable to schedule due
   * to insufficient applicable allocations
   */
  val shortfall: Duration,
)

data class SchedulePlan(
  val schedulingId: UUID,
  val crn: String,
  val eventNumber: Int,
  val actions: List<SchedulingAction>,
  val shortfall: Duration,
) {
  companion object
}

sealed interface SchedulingAction {
  data class CreateAppointment(
    val toCreate: SchedulingRequiredAppointment,
  ) : SchedulingAction

  // RetainAppointment is currently a 'no-op' action, provided for information/logging only
  data class RetainAppointment(
    val toRetain: SchedulingExistingAppointment,
    val notes: String,
  ) : SchedulingAction
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class SchedulingRequest(
  val today: LocalDate,
  val trigger: SchedulingTrigger,
  val requirement: SchedulingRequirement,
  val allocations: SchedulingAllocations,
  val existingAppointments: SchedulingExistingAppointments,
  val nonWorkingDates: SchedulingNonWorkingDates,
) {
  companion object
}

enum class SchedulingTrigger {
  AppointmentUpdate,
}

data class SchedulingRequirement(
  /**
   * The corresponding community payback requirement length, before any
   * time is created. This will include adjustments
   */
  val length: Duration,
) {
  companion object
}

data class SchedulingProject(
  val code: String,
) {
  companion object
}

enum class SchedulingFrequency {
  ONCE,
  WEEKLY,
  FORTNIGHTLY,
}

data class SchedulingAllocations(
  val allocations: List<SchedulingAllocation>,
)

/**
 * Derived by combining values from the requirement's Allocations
 * with any associated Availability and Project
 */
data class SchedulingAllocation(
  val alias: String?,
  val project: SchedulingProject,
  /**
   * The longest frequency of the allocation and availability, or weekly if neither is defined
   */
  val frequency: SchedulingFrequency,
  val dayOfWeek: DayOfWeek,
  val startDateInclusive: LocalDate,
  /**
   * The case allocation end date (inclusive), or if that isn't set, the earliest of availability end date,
   * project expected end date or project actual end date (if any are defined, all exclusive)
   */
  val endDateInclusive: LocalDate?,
  val startTime: LocalTime,
  val endTime: LocalTime,
) {
  companion object
}

data class SchedulingExistingAppointments(
  val appointments: List<SchedulingExistingAppointment>,
)

data class SchedulingExistingAppointment(
  val project: SchedulingProject,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val hasOutcome: Boolean,
  val timeCredited: Duration?,
  val allocation: SchedulingAllocation?,
) {
  companion object
}

data class SchedulingRequiredAppointment(
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
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
  val appointments: List<SchedulingRequiredAppointment>,
  /**
   * The minutes we were unable to schedule
   */
  val shortfall: Duration,
)

data class SchedulePlan(
  val actions: List<SchedulingAction>,
)

sealed interface SchedulingAction {
  data class SchedulingActionNewAppointment(
    val toCreate: SchedulingRequiredAppointment,
  ) : SchedulingAction
}

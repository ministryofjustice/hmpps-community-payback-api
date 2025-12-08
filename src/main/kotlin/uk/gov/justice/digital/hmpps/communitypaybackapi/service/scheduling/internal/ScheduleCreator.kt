package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.Duration
import java.time.LocalDate

object ScheduleCreator {
  private val log = LoggerFactory.getLogger(this::class.java)

  const val HARD_LIMIT_DAYS: Long = 365 * 5

  @SuppressWarnings("MagicNumber")
  fun createSchedule(
    schedulingRequest: SchedulingRequest,
    remainingMinutesToBeScheduled: Duration,
  ): Schedule {
    val today = schedulingRequest.today
    val hardLimit = today.plusDays(HARD_LIMIT_DAYS)
    val nonWorkingDates = schedulingRequest.nonWorkingDates

    val ctx = SchedulingContext(
      existingAppointments = schedulingRequest.existingAppointments,
      allocations = schedulingRequest.allocations,
      remainingMinutesToBeScheduled = remainingMinutesToBeScheduled,
    )

    var dayBeingConsidered = nonWorkingDates.nextWorkingDayAsOf(today.minusDays(1))
    while (
      ctx.moreAppointmentsRequired() &&
      ctx.getAllocations().anyAppointmentsOnOrAfter(ctx.getScheduledAppointments(), dayBeingConsidered)
    ) {
      scheduleDay(ctx, dayBeingConsidered)

      dayBeingConsidered = nonWorkingDates.nextWorkingDayAsOf(dayBeingConsidered)
      if (dayBeingConsidered.isAfter(hardLimit)) {
        log.warn("Scheduling is still being applied after $HARD_LIMIT_DAYS days. This shouldn't really happen. Won't attempt to schedule anything else.")
        break
      }
    }

    return ctx.toSchedule()
  }

  private fun SchedulingNonWorkingDates.nextWorkingDayAsOf(asOf: LocalDate): LocalDate {
    val candidate = asOf.plusDays(1)
    return if (!dates.contains(candidate)) {
      candidate
    } else {
      nextWorkingDayAsOf(candidate)
    }
  }

  private fun scheduleDay(
    ctx: SchedulingContext,
    day: LocalDate,
  ) {
    log.trace("Considering day {} ({}) with a remaining short fall of {}", day, day.dayOfWeek, ctx.getRemainingMinutesToBeScheduled())

    if (!ctx.getAllocations().anyAppointmentsOn(ctx.getScheduledAppointments(), day)) {
      return
    }

    /*
     This is emulating unexpected behaviour in the ND scheduler related to what happens
     if an appointment exists already when trying to allocate to a given day. In this case:

     1. Do not schedule an appointment
     2. If the existing appointment doesn't have an outcome, credit its pending time
     */
    val existingAppointmentsToday = ctx.getExistingAppointments().filter { it.date == day }
    if (existingAppointmentsToday.isNotEmpty()) {
      existingAppointmentsToday.filter { !it.hasOutcome }.forEach {
        ctx.addAppointmentToSchedule(
          SchedulingRequiredAppointment(
            date = it.date,
            startTime = it.startTime,
            endTime = it.endTime,
            project = it.project,
            allocation = it.allocation,
          ),
        )
      }
    } else {
      val sortedAllocations = ctx.getAllocations().allocations.sortedBy { it.startTime }
      sortedAllocations.forEach { allocation ->
        if (
          ctx.moreAppointmentsRequired() &&
          allocation.nextAppointmentOnOrAfter(ctx.getScheduledAppointments(), day) == day
        ) {
          scheduleAppointment(ctx, allocation, day)
        }
      }
    }
  }

  private fun scheduleAppointment(
    ctx: SchedulingContext,
    allocation: SchedulingAllocation,
    day: LocalDate,
  ) {
    val duration = shortestOf(ctx.getRemainingMinutesToBeScheduled(), allocation.duration())

    val startTime = allocation.startTime
    val endTime = startTime.plus(duration)

    ctx.addAppointmentToSchedule(
      SchedulingRequiredAppointment(
        date = day,
        startTime = startTime,
        endTime = endTime,
        project = allocation.project,
        allocation = allocation,
      ),
    )
  }

  class SchedulingContext(
    private val existingAppointments: SchedulingExistingAppointments,
    private val allocations: SchedulingAllocations,
    private var remainingMinutesToBeScheduled: Duration,
    private val schedule: MutableList<SchedulingRequiredAppointment> = mutableListOf(),
  ) {
    fun getExistingAppointments() = existingAppointments.appointments.toList()

    fun getAllocations() = allocations

    fun moreAppointmentsRequired() = remainingMinutesToBeScheduled.toMinutes() > 0

    fun getRemainingMinutesToBeScheduled() = remainingMinutesToBeScheduled

    fun addAppointmentToSchedule(appointment: SchedulingRequiredAppointment) {
      schedule.add(appointment)

      remainingMinutesToBeScheduled -= minutesBetween(appointment.startTime, appointment.endTime)
    }

    fun getScheduledAppointments() = schedule.toList()

    fun toSchedule() = Schedule(
      appointments = getScheduledAppointments(),
      shortfall = remainingMinutesToBeScheduled,
    )
  }
}

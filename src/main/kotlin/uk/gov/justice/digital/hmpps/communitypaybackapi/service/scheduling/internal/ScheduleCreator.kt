package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.minutesBetween
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.shortestOf
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.Duration
import java.time.LocalDate

object ScheduleCreator {
  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * We will only schedule up to 5 years of appointments. Typically, requirements
   * should only last a year so anything significantly larger than this implies
   * the provided requirement length is invalid (something which NDelius should
   * be limiting regardless).
   */
  const val CUT_OFF_LIMIT_DAYS: Long = 365 * 5

  /**
   * Create a schedule of appointments for today and in the future required to satisfy the remaining time (or as much of the time as possible)
   */
  fun createSchedule(
    schedulingRequest: SchedulingRequest,
    remainingMinutesToBeScheduled: Duration,
  ): Schedule {
    val today = schedulingRequest.today
    val cutOffDate = today.plusDays(CUT_OFF_LIMIT_DAYS)
    val nonWorkingDates = schedulingRequest.nonWorkingDates

    val ctx = SchedulingContext(
      existingAppointments = schedulingRequest.existingAppointments,
      allocations = schedulingRequest.allocations,
      remainingMinutesToBeScheduled = remainingMinutesToBeScheduled,
    )

    val startingDay = nonWorkingDates.nextWorkingDayAsOf(today.minusDays(1))
    var dayBeingConsidered = startingDay
    while (
      ctx.moreAppointmentsRequired() &&
      ctx.getAllocations().anyPotentialAppointmentsOnOrAfter(
        scheduledAppointments = ctx.getScheduledAppointments(),
        onOrAfter = dayBeingConsidered,
      )
    ) {
      scheduleDay(ctx, dayBeingConsidered)

      dayBeingConsidered = nonWorkingDates.nextWorkingDayAsOf(dayBeingConsidered)
      if (dayBeingConsidered.isAfter(cutOffDate)) {
        log.warn("Scheduling is still being applied after $CUT_OFF_LIMIT_DAYS days - this shouldn't really happen! We won't attempt to schedule anything else.")
        break
      }
    }

    return ctx.toSchedule()
  }

  private tailrec fun SchedulingNonWorkingDates.nextWorkingDayAsOf(asOf: LocalDate): LocalDate {
    val candidate = asOf.plusDays(1)
    return if (candidate !in dates) candidate else nextWorkingDayAsOf(candidate)
  }

  private fun scheduleDay(
    ctx: SchedulingContext,
    day: LocalDate,
  ) {
    log.trace("Scheduling day {} ({}) with a remaining short fall of {}", day, day.dayOfWeek, ctx.getRemainingMinutesToBeScheduled())

    if (!ctx.getAllocations().anyPotentialAppointmentsOn(day, ctx.getScheduledAppointments())) {
      return
    }

    /*
     This is emulating unexpected behaviour in the ND scheduler related to what happens
     if an appointment exists already when trying to allocate to a given day. In this case:

     1. Do not schedule an appointment
     2. If the existing appointment doesn't have an outcome, add it to the schedule
     */
    val existingAppointmentsToday = ctx.getExistingAppointments().filter { it.date == day }
    if (existingAppointmentsToday.isNotEmpty()) {
      existingAppointmentsToday.filter { !it.hasOutcome }.forEach {
        ctx.addForcedRetentionAppointment(it)
      }
    } else {
      val allocationsEarliestTimeFirst = ctx.getAllocations().allocations.sortedBy { it.startTime }
      allocationsEarliestTimeFirst.forEach { allocation ->
        if (
          ctx.moreAppointmentsRequired() &&
          allocation.nextPotentialAppointmentDateOnOrAfter(day, ctx.getScheduledAppointments()) == day
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
    private val forcedRetentions: MutableList<SchedulingExistingAppointment> = mutableListOf(),
  ) {
    fun getExistingAppointments() = existingAppointments.appointments.toList()

    fun getAllocations() = allocations

    fun moreAppointmentsRequired() = remainingMinutesToBeScheduled.toMinutes() > 0

    fun getRemainingMinutesToBeScheduled() = remainingMinutesToBeScheduled

    fun addAppointmentToSchedule(appointment: SchedulingRequiredAppointment) {
      schedule.add(appointment)

      remainingMinutesToBeScheduled -= minutesBetween(appointment.startTime, appointment.endTime)
    }

    fun addForcedRetentionAppointment(appointment: SchedulingExistingAppointment) {
      forcedRetentions.add(appointment)

      remainingMinutesToBeScheduled -= minutesBetween(appointment.startTime, appointment.endTime)
    }

    fun getScheduledAppointments() = schedule.toList()

    fun toSchedule() = Schedule(
      requiredAppointmentsAsOfToday = getScheduledAppointments(),
      forcedRetentions = forcedRetentions,
      shortfall = remainingMinutesToBeScheduled,
    )
  }

  private fun SchedulingAllocation.duration() = minutesBetween(startTime, endTime)
}

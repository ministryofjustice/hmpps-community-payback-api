package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.minutesBetween
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.shortestOf
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingTriggerType
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
      schedulingRequest = schedulingRequest,
      remainingMinutesToBeScheduled = remainingMinutesToBeScheduled,
    )

    val startingDay = nonWorkingDates.nextWorkingDayAsOf(today.minusDays(1))
    var dayBeingConsidered = startingDay
    while (
      ctx.moreAppointmentsRequired() &&
      ctx.getAllocations().anyPotentialAppointmentsOnOrAfter(
        alreadyScheduledAppointments = ctx.getScheduledAppointments(),
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

  private fun scheduleDay(
    ctx: SchedulingContext,
    day: LocalDate,
  ) {
    log.trace("Scheduling day {} ({}) with a remaining short fall of {}", day, day.dayOfWeek, ctx.getRemainingMinutesToBeScheduled())

    if (!ctx.getAllocations().anyPotentialAppointmentsOn(day, ctx.getScheduledAppointments())) {
      return
    }

    val existingAppointmentsToday = ctx.getExistingAppointments().filter { it.date == day }
    /*
     If scheduling is triggered by an appointment change and appointment(s) already exists
     on a day we want to schedule an appointment on, we retain those existing appointments
     in the schedule. This broadly matches NDelius behaviour with some exceptions, which
     can lead to different scheduling outcomes when compared to NDelius. Most notably,
     when calculating the remaining minutes to schedule, our implementation considers _all_
     existing appointments, where as NDelius only considers one of the existing appointments
     (indeterminately).
     */
    if (ctx.getTriggerType() == SchedulingTriggerType.AppointmentChange && existingAppointmentsToday.isNotEmpty()) {
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

  private tailrec fun SchedulingNonWorkingDates.nextWorkingDayAsOf(asOf: LocalDate): LocalDate {
    val candidate = asOf.plusDays(1)
    return if (candidate !in dates) candidate else nextWorkingDayAsOf(candidate)
  }

  class SchedulingContext(
    private val schedulingRequest: SchedulingRequest,
    private var remainingMinutesToBeScheduled: Duration,
    private val schedule: MutableList<SchedulingRequiredAppointment> = mutableListOf(),
    private val forcedRetentions: MutableList<SchedulingExistingAppointment> = mutableListOf(),
  ) {
    fun getExistingAppointments() = schedulingRequest.existingAppointments.appointments.toList()

    fun getAllocations() = schedulingRequest.allocations

    fun getTriggerType() = schedulingRequest.trigger.type

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

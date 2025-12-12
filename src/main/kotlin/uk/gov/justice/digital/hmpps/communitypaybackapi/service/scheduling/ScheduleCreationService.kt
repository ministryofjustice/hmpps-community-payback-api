package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

object ScheduleCreationService {
  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("MagicNumber")
  fun createSchedule(
    schedulingRequest: SchedulingRequest,
    remainingMinutesToBeScheduled: Duration,
  ): Schedule {
    log.debug("{} minutes to be scheduled ", remainingMinutesToBeScheduled)

    val allocations = schedulingRequest.allocations
    val nonWorkingDates = schedulingRequest.nonWorkingDates.dates
    val today = schedulingRequest.today

    val ctx = SchedulingContext(remainingMinutesToBeScheduled)

    log.debug("There are ${allocations.allocations.size} allocations")
    var dayBeingConsidered = today
    val ceiling = today.plusDays(365 * 5)
    while (ctx.moreAppointmentsRequired() && allocations.anyAppointmentOnOrAfter(ctx, dayBeingConsidered)) {
      scheduleDay(
        ctx = ctx,
        allocations = allocations,
        day = dayBeingConsidered,
      )

      do {
        dayBeingConsidered = dayBeingConsidered.plusDays(1)
      } while (nonWorkingDates.contains(dayBeingConsidered))

      if (dayBeingConsidered.isAfter(ceiling)) {
        log.warn("Scheduling overflow")
        break
      }
    }

    return ctx.toSchedule()
  }

  private fun scheduleDay(
    ctx: SchedulingContext,
    allocations: SchedulingAllocations,
    day: LocalDate,
  ) {
    log.trace("Considering day {} ({}) with a remaining short fall of {}", day, day.dayOfWeek, ctx.getRemainingMinutesToBeScheduled())

    allocations.allocations.sortedBy { it.startTime }.forEach { allocation ->
      if (allocation.nextAppointmentOnOrAfter(ctx.getScheduledAppointments(), day) == day) {
        val appointment = createAppointment(ctx, allocation, day)

        ctx.addAppointment(appointment)

        appointment.logAppointmentScheduled()

        if (ctx.noMoreAppointmentsRequired()) {
          return
        }
      }
    }
  }

  private fun createAppointment(
    ctx: SchedulingContext,
    allocation: SchedulingAllocation,
    day: LocalDate,
  ): SchedulingRequiredAppointment {
    val duration = shortestOf(ctx.getRemainingMinutesToBeScheduled(), allocation.duration())

    val startTime = allocation.startTime
    val endTime = startTime.plus(duration)

    return SchedulingRequiredAppointment(
      date = day,
      startTime = startTime,
      endTime = endTime,
      allocation = allocation,
    )
  }

  private fun SchedulingRequiredAppointment.logAppointmentScheduled() {
    log.trace(
      "Scheduling appointment on {} for alloc {} (project {} starting {} ending {}{})",
      date,
      allocation.alias,
      allocation.project.code,
      startTime,
      endTime,
      if (endTime != allocation.endTime) {
        " truncated"
      } else {
        ""
      },
    )
  }

  class SchedulingContext(
    private var remainingMinutesToBeScheduled: Duration,
    private val scheduledAppointments: MutableList<SchedulingRequiredAppointment> = mutableListOf(),
  ) {
    fun noMoreAppointmentsRequired() = !moreAppointmentsRequired()

    fun moreAppointmentsRequired() = remainingMinutesToBeScheduled.toMinutes() > 0

    fun getRemainingMinutesToBeScheduled() = remainingMinutesToBeScheduled

    fun addAppointment(appointment: SchedulingRequiredAppointment) {
      scheduledAppointments.add(appointment)
      remainingMinutesToBeScheduled -= minutesBetween(appointment.startTime, appointment.endTime)
    }

    fun getScheduledAppointments() = scheduledAppointments.toList()

    fun toSchedule() = Schedule(
      appointments = getScheduledAppointments(),
      shortfall = remainingMinutesToBeScheduled,
    )
  }

  private fun SchedulingAllocations.anyAppointmentOnOrAfter(ctx: SchedulingContext, onOrAfter: LocalDate) = allocations.any { it.nextAppointmentOnOrAfter(ctx.getScheduledAppointments(), onOrAfter) != null }

  private fun SchedulingAllocation.duration() = minutesBetween(startTime, endTime)

  private fun shortestOf(duration1: Duration, duration2: Duration) = if (duration1 < duration2) {
    duration1
  } else {
    duration2
  }
}

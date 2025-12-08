package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SchedulingRenderer {

  fun renderRequestSummary(
    title: String,
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
  ): String {
    val today = request.today

    return buildString {
      appendLine()
      appendLine()
      appendLine("# $title")
      appendLine()
      appendLine("* Date: $today (${today.dayOfWeek})")
      appendLine("* Trigger: ${request.trigger}")
      appendLine("* Requirement: ${request.requirement.length}")
      appendLine("* Remaining Minutes as of today: $remainingMinutesAsOfToday")

      val allocations = request.allocations.allocations
      if (allocations.isNotEmpty()) {
        appendLine("* Allocations (${allocations.size}):")
        appendLine("```")
        appendLine(allocations.joinToString("\n") { renderAllocation(today, it) })
        appendLine("```")
      } else {
        appendLine("* Allocations (0)")
      }

      val existingAppointments = request.existingAppointments.appointments
      if (existingAppointments.isNotEmpty()) {
        appendLine("* Existing Appointments (${existingAppointments.size}):")
        appendLine("```")
        appendLine(existingAppointments.joinToString("\n") { renderExistingAppointment(today, it) })
        appendLine("```")
      } else {
        appendLine("* Existing Appointments (0)")
      }

      append("* Non Working Dates: ")
      appendLine(request.nonWorkingDates.dates)
    }
  }

  fun renderNoChangesRequired(
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
  ) = buildString {
    append(renderRequestSummary("Scheduling Results", request, remainingMinutesAsOfToday))
    appendLine(" ** No changes required **")
    appendLine()
  }

  fun renderScheduleAndPlan(
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
    schedule: Schedule,
    plan: SchedulePlan,
  ): String {
    val today = request.today

    return buildString {
      append(renderRequestSummary("Scheduling Results", request, remainingMinutesAsOfToday))

      val shortfall = schedule.shortfall
      if (!shortfall.isZero) {
        appendLine()
        appendLine("!!! The Schedule has a shortfall of $shortfall !!!")
        appendLine()
      }

      val scheduleAppointments = schedule.appointments
      if (scheduleAppointments.isNotEmpty()) {
        appendLine("* Schedule (${scheduleAppointments.size}):")
        appendLine("```")
        appendLine(scheduleAppointments.joinToString("\n") { renderRequiredAppointment(today, it) })
        appendLine("```")
      } else {
        appendLine("* Schedule (0)")
      }

      // DA: add actual date into the output
      val actions = plan.actions
      if (actions.isNotEmpty()) {
        appendLine("* Plan to achieve schedule (${actions.size}):")
        appendLine("```")
        appendLine(actions.joinToString("\n") { renderAction(today, it) })
        appendLine("```")
      } else {
        appendLine("* Plan to achieve schedule (0)")
      }

      appendLine("")
    }
  }

  private fun renderAllocation(
    today: LocalDate,
    allocation: SchedulingAllocation,
  ): String {
    // "ALLOC1-PROJ1-WK-MON-10:00-18:00, Starting Today+1"
    return buildString {
      append(allocation.alias)
      append("-")
      append(allocation.project.code)
      append("-")
      append(
        when (allocation.frequency) {
          SchedulingFrequency.ONCE -> "ONCE"
          SchedulingFrequency.WEEKLY -> "WK"
          SchedulingFrequency.FORTNIGHTLY -> "FN"
        },
      )
      append("-")
      append(
        when (allocation.dayOfWeek) {
          DayOfWeek.MONDAY -> "MON"
          DayOfWeek.TUESDAY -> "TUE"
          DayOfWeek.WEDNESDAY -> "WED"
          DayOfWeek.THURSDAY -> "THU"
          DayOfWeek.FRIDAY -> "FRI"
          DayOfWeek.SATURDAY -> "SAT"
          DayOfWeek.SUNDAY -> "SUN"
        },
      )
      append("-")
      append(allocation.startTime)
      append("-")
      append(allocation.endTime)
      append(", Starting ")
      append(dayAsDelta(today, allocation.startDateInclusive))
      allocation.endDateInclusive?.let {
        append(", Ending ")
        append(dayAsDelta(today, it))
      }
    }
  }

  private fun renderExistingAppointment(
    today: LocalDate,
    existingAppointment: SchedulingExistingAppointment,
  ): String {
    // "Today-6, ALLOC2, 16:00-20:00, Pending"
    // "Today-6, ALLOC2, 16:00-20:00, Credited PT5H30M"
    return buildString {
      append(dayAsDelta(today, existingAppointment.date))
      append(", ")
      append(existingAppointment.allocation?.alias)
      append(", ")
      append(existingAppointment.startTime)
      append("-")
      append(existingAppointment.endTime)
      append(", ")
      if (!existingAppointment.hasOutcome) {
        append("Pending")
      } else {
        // DA: can we make this match the input terminology?
        append("Complete ")
        append(existingAppointment.timeCredited ?: Duration.ZERO)
      }
    }
  }

  private fun renderRequiredAppointment(
    today: LocalDate,
    requiredAppointment: SchedulingRequiredAppointment,
  ): String {
    // "Today+6, ALLOC1, 16:00-20:00"
    return buildString {
      append(dayAsDelta(today, requiredAppointment.date))
      append(", ")
      append(requiredAppointment.allocation.alias)
      append(", ")
      append(requiredAppointment.startTime)
      append("-")
      append(requiredAppointment.endTime)
    }
  }

  private fun renderAction(
    today: LocalDate,
    action: SchedulingAction,
  ): String = when (action) {
    // "Create, Today+6, ALLOC1, 16:00-20:00"
    is SchedulingAction.SchedulingActionNewAppointment -> {
      val appointment = action.toCreate
      val allocation = appointment.allocation
      buildString {
        append("Create, ")
        append(dayAsDelta(today, appointment.date))
        append(", ")
        append(allocation.alias)
        append(", ")
        append(appointment.startTime)
        append("-")
        append(appointment.endTime)
        if (appointment.allocation.endTime != appointment.endTime) {
          append(" (Truncated from allocation end time ${allocation.endTime})")
        }
      }
    }
  }

  private fun dayAsDelta(baseline: LocalDate, dateToShow: LocalDate): String {
    val delta = ChronoUnit.DAYS.between(baseline, dateToShow)
    return if (delta == 0L) {
      "TODAY"
    } else if (delta > 0) {
      "TODAY+$delta"
    } else {
      "TODAY$delta"
    }
  }
}

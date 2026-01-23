package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.Schedule
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulePlan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object SchedulingRenderer {

  fun requestSummary(
    title: String,
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
  ): String {
    val today = request.today
    val trigger = request.trigger

    return buildString {
      appendLine()
      appendLine()
      appendLine("# $title")
      appendLine()
      appendLine("* Date: $today (${today.dayOfWeek})")
      appendLine("* Trigger: ${trigger.type} : ${trigger.description}")
      appendLine("* CRN: ${request.requirement.crn}")
      appendLine("* Delius Event No: ${request.requirement.deliusEventNumber}")
      appendLine("* Requirement Length (includes adjustments): ${request.requirement.requirementLengthMinutes}")
      appendLine("* Remaining Minutes as of today: $remainingMinutesAsOfToday")

      val allocations = request.allocations.allocations
      if (allocations.isNotEmpty()) {
        appendLine("* Allocations (${allocations.size}):")
        appendLine("```")
        appendLine(allocations.joinToString("\n") { it.render(today) })
        appendLine("```")
      } else {
        appendLine("* Allocations (0)")
      }

      val existingAppointments = request.existingAppointments.appointments
      if (existingAppointments.isNotEmpty()) {
        appendLine("* Existing Appointments (${existingAppointments.size}):")
        appendLine("```")
        appendLine(existingAppointments.joinToString("\n") { it.render(today) })
        appendLine("```")
      } else {
        appendLine("* Existing Appointments (0)")
      }

      append("* Non Working Dates as of Scheduling Date: ")
      appendLine(
        request.nonWorkingDates.dates
          .filter { it.onOrAfter(request.today) }
          .sorted(),
      )
    }
  }

  fun noChangesRequired(
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
  ) = buildString {
    append(requestSummary("Scheduling Results", request, remainingMinutesAsOfToday))
    appendLine(" ** No changes required **")
    appendLine()
  }

  fun scheduleAndPlan(
    request: SchedulingRequest,
    remainingMinutesAsOfToday: Duration,
    schedule: Schedule,
    plan: SchedulePlan,
  ): String {
    val today = request.today

    return buildString {
      append(requestSummary("Scheduling Results", request, remainingMinutesAsOfToday))

      val shortfall = schedule.shortfall
      if (!shortfall.isZero && shortfall.isPositive) {
        appendLine()
        appendLine("!!! The Schedule has a shortfall of $shortfall !!!")
        appendLine()
      }

      val scheduleAppointments = schedule.requiredAppointmentsAsOfToday
      if (scheduleAppointments.isNotEmpty()) {
        appendLine("* Schedule (${scheduleAppointments.size}):")
        appendLine("```")
        appendLine(scheduleAppointments.joinToString("\n") { it.render(today) })
        appendLine("```")
      } else {
        appendLine("* Schedule (0)")
      }

      val actions = plan.actions
      if (actions.isNotEmpty()) {
        appendLine("* Plan to achieve schedule (${actions.size}):")
        appendLine("```")
        appendLine(
          actions.sortedBy {
            when (it) {
              is SchedulingAction.CreateAppointment -> it.toCreate.date.atTime(it.toCreate.startTime)
              is SchedulingAction.RetainAppointment -> it.toRetain.date.atTime(it.toRetain.startTime)
            }
          }.joinToString("\n") { it.render(today) },
        )
        appendLine("```")
      } else {
        appendLine("* Plan to achieve schedule (0)")
      }
    }
  }

  // "ALLOC1-PROJ1-WK-MON-10:00-18:00, Starting Today+1"
  private fun SchedulingAllocation.render(today: LocalDate) = buildString {
    append(alias)
    append("-")
    append(project.code)
    append("-")
    append(
      when (frequency) {
        SchedulingFrequency.ONCE -> "ONCE"
        SchedulingFrequency.WEEKLY -> "WK"
        SchedulingFrequency.FORTNIGHTLY -> "FN"
      },
    )
    append("-")
    append(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase())
    append("-")
    append(startTime)
    append("-")
    append(endTime)
    append(", Starting ")
    append(dayAsDelta(today, startDateInclusive))
    endDateInclusive?.let {
      append(", Ending ")
      append(dayAsDelta(today, it))
    }
  }

  // "Today-6, Alloc ALLOC2, 16:00-20:00, Pending"
  // "Today-6, Alloc ALLOC2, 16:00-20:00, Credited PT5H30M"
  private fun SchedulingExistingAppointment.render(
    today: LocalDate,
  ) = buildString {
    append(dayAsDelta(today, date))
    append(", ")
    append(allocationId?.let { "Alloc $it" } ?: "No Allocation")
    append(", ")
    append(startTime)
    append("-")
    append(endTime)
    append(", ")
    if (!hasOutcome) {
      append("No Outcome")
    } else {
      append("Mins Credited ")
      append(minutesCredited)
    }
  }

  // "28f0, Today+6, ALLOC1, 16:00-20:00"
  @SuppressWarnings("MagicNumber")
  private fun SchedulingRequiredAppointment.render(today: LocalDate) = buildString {
    append(reference.toString().substring(0, 4))
    append(", ")
    append(dayAsDelta(today, date))
    append(", ")
    append(project.code)
    append(", ")
    append(allocation.alias)
    append(", ")
    append(startTime)
    append("-")
    append(endTime)
    if (allocation.endTime != endTime) {
      append(" (Truncated from allocation end time ${allocation.endTime})")
    }
  }

  private fun SchedulingAction.render(today: LocalDate) = when (this) {
    is SchedulingAction.CreateAppointment -> {
      buildString {
        append("Create - ")
        append(toCreate.render(today))
      }
    }
    is SchedulingAction.RetainAppointment -> {
      buildString {
        append("Retain - ")
        append(toRetain.render(today))
        append(" (")
        append(notes)
        append(")")
      }
    }
  }

  private fun dayAsDelta(baseline: LocalDate, dateToShow: LocalDate) = buildString {
    val delta = ChronoUnit.DAYS.between(baseline, dateToShow)

    append(
      when {
        delta == 0L -> "TODAY"
        delta > 0 -> "TODAY+$delta"
        else -> "TODAY$delta"
      },
    )

    append(" (")
    append(dateToShow)
    append(")")
  }
}

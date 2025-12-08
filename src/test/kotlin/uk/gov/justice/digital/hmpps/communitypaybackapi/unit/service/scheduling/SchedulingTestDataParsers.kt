package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.regex.Matcher
import java.util.regex.Pattern

class SchedulingTestDataParsers {

  companion object {
    const val TIME_PATTERN: String = "[012]\\d:[012345]\\d"

    // "ALLOC1-PROJ1-WK-MON-12:00-20:00[, Starting Today[+/-1]][, Ending Today[+/-1]]"
    val ALLOCATION_PATTERN: Pattern = Pattern.compile(
      "(.+)-(.+)-(ONCE|WK|FN)-(MON|TUE|WED|THU|FRI|SAT|SUN|\\(TODAY-\\d\\))-($TIME_PATTERN)-($TIME_PATTERN)(, Starting Today[^,]*|[^,]*)(, Ending Today.*|.*)",
    )

    // DA: i think non attendance is too specific. Could be Credited 0?
    // 'Today-6, PROJ1, ALLOC2, 16:00-20:00, Non-attended'," +
    // 'Today-6, PROJ2, ALLOC2, 16:00-20:00, Credited PT5H30M'"
    val EXISTING_APPOINTMENT_PATTERN: Pattern = Pattern.compile(
      "(.+), (.+), (.+), ($TIME_PATTERN)-($TIME_PATTERN), (Pending|Credited PT.+|Non-attended)",
    )

    // DA: i don't like how the patterns differ wrt.use of hyphen and commas
    // Create, Today, ALLOC1, 10:00-18:00
    // Create, Today-1, ALLOC1, 10:00-18:00
    // Create, Today+1, ALLOC1, 10:00-18:00
    val PLAN_ACTION: Pattern = Pattern.compile(
      "(Create), (Today|Today\\+\\d+|Today-\\d+), (.*), ($TIME_PATTERN)-($TIME_PATTERN)",
    )
  }

  fun parseAllocationDescription(
    today: LocalDate,
    projects: List<SchedulingProject>,
    description: String,
  ): SchedulingAllocation {
    val matcher = ALLOCATION_PATTERN.matcher(description)

    val groups = if (matcher.matches() && matcher.groupCount() == 8) {
      matcher.collectGroups()
    } else {
      error(
        "Allocation description '$description' is invalid. " +
          "Should be in the format 'ALLOC1-PROJ1-ONCE|WK|FN-MON-10:00-18:00[, Starting Today+1][, Ending Today+1]'",
      )
    }

    val aliasPart = groups[0]
    val projectCodePart = groups[1]
    val frequencyPart = groups[2]
    val dayOfWeekPart = groups[3]
    val startTimePart = groups[4]
    val endTimePart = groups[5]
    val startDatePart = groups[6]
    val endDatePart = groups[7]

    return SchedulingAllocation(
      alias = aliasPart,
      project = projects.findByCode(projectCodePart),
      frequency = frequencyPart.toFrequency(),
      dayOfWeek = dayOfWeekPart.toDayOfWeek(),
      startDateInclusive = if (startDatePart.isNotBlank()) {
        dateOffsetToLocalDate(today, startDatePart.substring(", Starting ".length))
      } else {
        today
      },
      endDateInclusive = if (endDatePart.isNotBlank()) {
        dateOffsetToLocalDate(today, endDatePart.substring(", Ending ".length))
      } else {
        null
      },
      startTime = LocalTime.parse(startTimePart),
      endTime = LocalTime.parse(endTimePart),
    )
  }

  fun String.toFrequency() = when (this) {
    "WK" -> SchedulingFrequency.WEEKLY
    "FN" -> SchedulingFrequency.FORTNIGHTLY
    "ONCE" -> SchedulingFrequency.ONCE
    else -> error("Can't parse frequency '$this'")
  }

  fun String.toDayOfWeek() = when (this) {
    "MON" -> MONDAY
    "TUE" -> TUESDAY
    "WED" -> WEDNESDAY
    "THU" -> THURSDAY
    "FRI" -> FRIDAY
    "SAT" -> SATURDAY
    "SUN" -> SUNDAY
    else -> error("Can't parse day '$this'")
  }

  fun parseExistingAppointmentDescription(
    today: LocalDate,
    allocations: List<SchedulingAllocation>,
    projects: List<SchedulingProject>,
    description: String,
  ): SchedulingExistingAppointment {
    val matcher = EXISTING_APPOINTMENT_PATTERN.matcher(description)

    val group = if (matcher.matches() && matcher.groupCount() == 6) {
      matcher.collectGroups()
    } else {
      error(
        "Existing Appointment description '$description' is invalid. Should be in one of the following formats " +
          "'Today-6, PROJ1, ALLOC2, 16:00-20:00, Pending'," +
          "'Today-6, PROJ1, ALLOC2, 16:00-20:00, Credited PT5H30M'",
      )
    }

    val dateOffset = group[0]
    val projectCode = group[1]
    val allocationAlias = group[2]
    val startTime = group[3]
    val endTime = group[4]
    val status = group[5]

    val date = dateOffsetToLocalDate(today, dateOffset)

    val allocation = if (allocationAlias == "MANUAL") {
      null
    } else {
      allocations.firstOrNull { it.alias == allocationAlias }
        ?: error("Couldn't find an allocation with alias '$allocationAlias'")
    }

    val timeCredited = if (status.startsWith("Credited")) {
      Duration.parse(status.substring("Credited ".length))
    } else {
      null
    }

    return SchedulingExistingAppointment(
      project = projects.first { it.code == projectCode },
      date = date,
      startTime = LocalTime.parse(startTime),
      endTime = LocalTime.parse(endTime),
      hasOutcome = when (status) {
        "Pending" -> false
        else -> true
      },
      timeCredited = timeCredited,
      allocation = allocation,
    )
  }

  fun parseActionDescription(
    today: LocalDate,
    allocations: List<SchedulingAllocation>,
    description: String,
  ): SchedulingAction {
    val matcher = PLAN_ACTION.matcher(description)

    val group = if (matcher.matches() && matcher.groupCount() == 5) {
      matcher.collectGroups()
    } else {
      error(
        "Action description '$description' is invalid. " +
          "Should be in the format 'Create, Today[+/-123], ALLOC1, 10:00-18:00'",
      )
    }

    val type = group[0]
    val dateOffset = group[1]
    val allocationAlias = group[2]
    val startTime = group[3]
    val endTime = group[4]

    val allocation = allocations.firstOrNull { it.alias == allocationAlias }
      ?: error("Couldn't find an allocation with alias '$allocationAlias'")

    return when (type) {
      "Create" -> SchedulingAction.SchedulingActionNewAppointment(
        SchedulingRequiredAppointment(
          date = dateOffsetToLocalDate(today, dateOffset),
          startTime = LocalTime.parse(startTime),
          endTime = LocalTime.parse(endTime),
          allocation = allocation,
        ),
      )
      else -> error("Unknown type $type")
    }
  }

  fun parseNonWorkingDates(
    today: LocalDate,
    description: String,
  ) = dateOffsetToLocalDate(today, description)

  private fun dateOffsetToLocalDate(
    today: LocalDate,
    /**
     * Today
     * Today-1
     * Today+1
     */
    offsetDescription: String,
  ) = today.plusDays(
    if (offsetDescription.length == "Today".length) {
      0
    } else {
      val sub = offsetDescription.substring("Today".length)
      sub.toLong()
    },
  )

  private fun List<SchedulingProject>.findByCode(code: String) = firstOrNull { it.code == code } ?: error("Couldn't find project for code '$code'")

  private fun Matcher.collectGroups() = 1.rangeTo(groupCount()).map { group(it) }
}

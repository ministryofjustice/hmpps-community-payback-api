package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.ONCE
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.Duration

class SchedulingScenariosNonWorkingDatesTest {

  @Test
  fun `DATES-NWD-01 Once Frequency Ignored if Non Working Day`() {
    schedulingScenario {
      scenarioId("DATES-NWD-01")
      given {
        requirementIsHours(8)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(ONCE)
          onWeekDay(SATURDAY)
          from("10:00")
          until("18:00")
          startingToday()
          endingInDays(7)
        }

        nonWorkingDate(5)
      }

      then {
        noActionsExpected(toAddressShortfall = Duration.ofHours(8))
      }
    }
  }

  @Test
  fun `DATES-NWD-02 Week Frequency Skips Non Working Days`() {
    schedulingScenario {
      scenarioId("DATES-NWD-02")
      given {
        requirementIsHours(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
        }

        nonWorkingDate(7)
        nonWorkingDate(14)
      }

      then {
        shouldCreateAppointments {
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays()
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(21)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(28)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(35)
            from("10:00")
            until("18:00")
          }
        }
      }
    }
  }

  @Test
  fun `DATES-NWD-03 Fortnightly frequency skips non working day`() {
    schedulingScenario {
      scenarioId("DATES-NWD-03")
      given {
        requirementIsHours(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(FORTNIGHTLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
          startingToday()
        }

        nonWorkingDate(14)
      }

      then {
        shouldCreateAppointments {
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays()
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(28)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(42)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(56)
            from("10:00")
            until("18:00")
          }
        }
      }
    }
  }

  @Test
  fun `DATES-NWD-04 Ignore Non Working Day if it is Today`() {
    schedulingScenario {
      scenarioId("DATES-NWD-04")
      given {
        requirementIsHours(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
        }

        nonWorkingDate(0)
      }

      then {
        shouldCreateAppointments {
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(7)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(14)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(21)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(28)
            from("10:00")
            until("18:00")
          }
        }
      }
    }
  }
}

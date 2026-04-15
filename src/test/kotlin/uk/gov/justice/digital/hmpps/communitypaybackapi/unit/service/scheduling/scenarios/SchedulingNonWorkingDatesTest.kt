package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.ONCE
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.WEEKLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTriggerType
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.Duration

class SchedulingNonWorkingDatesTest {

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `DATES-NWD-01 Once Frequency Ignored if Non Working Day`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("DATES-NWD-01")
      given {
        requirementHoursAre(8)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        schedulingTriggerTypeIs(triggerType)

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

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `DATES-NWD-02 Week Frequency Skips Non Working Days`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("DATES-NWD-02")
      given {
        requirementHoursAre(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        schedulingTriggerTypeIs(triggerType)

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
            today()
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

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `DATES-NWD-03 Fortnightly frequency skips non working day`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("DATES-NWD-03")
      given {
        requirementHoursAre(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        schedulingTriggerTypeIs(triggerType)

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
            today()
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

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `DATES-NWD-04 Ignore Non Working Day if it is Today`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("DATES-NWD-04")
      given {
        requirementHoursAre(32)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        schedulingTriggerTypeIs(triggerType)

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

package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.WEEKLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTriggerType
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration

class SchedulingMiscTest {

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `MISC-01 Insufficient Allocations to meet requirements`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("MISC-01")
      given {
        requirementHoursAre(160)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")
        schedulingTriggerTypeIs(triggerType)

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
          endingInDays(29)
        }

        allocation {
          alias("ALLOC2")
          projectCode("PROJ2")
          frequency(WEEKLY)
          onWeekDay(WEDNESDAY)
          from("11:00")
          until("15:00")
          endingInDays(17)
        }
      }

      then {
        shouldCreateAppointments(toAddressShortfall = Duration.ofHours(108)) {
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            todayWithOffsetDays(2)
            from("11:00")
            until("15:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(7)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            todayWithOffsetDays(9)
            from("11:00")
            until("15:00")
          }
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(14)
            from("10:00")
            until("18:00")
          }
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            todayWithOffsetDays(16)
            from("11:00")
            until("15:00")
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

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `MISC-03 Maximum Requirement Length`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("MISC-03")
      given {
        requirementHoursAre(300)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        schedulingTriggerTypeIs(triggerType)

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("14:00")
        }
      }

      then {
        shouldCreateAppointments {
          (0..74).forEach { week ->
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(week * 7)
              from("10:00")
              until("14:00")
            }
          }
        }
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `MISC-04 If multiple allocations on same day, schedule earliest start time first`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("MISC-04")
      given {
        requirementHoursAre(8)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")
        schedulingTriggerTypeIs(triggerType)

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(WEDNESDAY)
          from("10:00")
          until("18:00")
        }

        allocation {
          alias("ALLOC2")
          projectCode("PROJ2")
          frequency(WEEKLY)
          onWeekDay(WEDNESDAY)
          from("08:00")
          until("16:00")
        }
      }

      then {
        shouldCreateAppointments {
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            todayWithOffsetDays(2)
            from("08:00")
            until("16:00")
          }
        }
      }
    }
  }
}

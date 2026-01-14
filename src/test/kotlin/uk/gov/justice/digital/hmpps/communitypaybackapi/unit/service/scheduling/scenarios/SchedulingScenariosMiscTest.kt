package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration

class SchedulingScenariosMiscTest {

  @Test
  fun `MISC-01 Insufficient Allocations to meet requirements`() {
    schedulingScenario {
      scenarioId("MISC-01")
      given {
        requirementIsHours(160)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")

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
            todayWithOffsetDays()
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

  @Test
  fun `MISC-03 Maximum Requirement Length`() {
    schedulingScenario {
      scenarioId("MISC-03")
      given {
        requirementIsHours(300)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

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

  @Test
  fun `MISC-04 If multiple allocations on same day, schedule earliest start time first`() {
    schedulingScenario {
      scenarioId("MISC-04")
      given {
        requirementIsHours(8)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")

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

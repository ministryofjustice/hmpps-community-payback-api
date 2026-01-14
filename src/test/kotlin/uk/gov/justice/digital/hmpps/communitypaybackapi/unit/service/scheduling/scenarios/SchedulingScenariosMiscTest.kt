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
      test("MISC-01")
      given {
        today(MONDAY)
        project("PROJ1")
        project("PROJ2")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(WEEKLY)
          on(MONDAY)
          from("10:00")
          until("18:00")
          endingIn(29)
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(WEDNESDAY)
          from("11:00")
          until("15:00")
          endingIn(17)
        }
      }

      whenScheduling {
        requirementIsHours(160)
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ2")
            allocation("ALLOC2")
            today(2)
            from("11:00")
            until("15:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(7)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ2")
            allocation("ALLOC2")
            today(9)
            from("11:00")
            until("15:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(14)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ2")
            allocation("ALLOC2")
            today(16)
            from("11:00")
            until("15:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(21)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(28)
            from("10:00")
            until("18:00")
          }
        }
        withShortfall(Duration.ofHours(108))
      }
    }
  }

  @Test
  fun `MISC-03 Maximum Requirement Length`() {
    schedulingScenario {
      test("MISC-03")
      given {
        today(MONDAY)
        project("PROJ1")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(WEEKLY)
          on(MONDAY)
          from("10:00")
          until("14:00")
        }
      }

      whenScheduling {
        requirementIsHours(300)
      }

      then {
        shouldCreateAppointments {
          (0..74).forEach { week ->
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(week * 7)
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
      test("MISC-04")
      given {
        today(MONDAY)
        project("PROJ1")
        project("PROJ2")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(WEEKLY)
          on(WEDNESDAY)
          from("10:00")
          until("18:00")
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(WEDNESDAY)
          from("08:00")
          until("16:00")
        }
      }

      whenScheduling {
        requirementIsHours(8)
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ2")
            allocation("ALLOC2")
            today(2)
            from("08:00")
            until("16:00")
          }
        }
      }
    }
  }
}

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
      test("DATES-NWD-01")
      given {
        today(MONDAY)
        project("PROJ1")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(ONCE)
          on(SATURDAY)
          from("10:00")
          until("18:00")
          startingToday()
          endingIn(7)
        }

        nonWorkingDate(5)
      }

      whenScheduling {
        requirementIsHours(8)
      }

      then {
        shouldCreateAppointments { }
        withShortfall(Duration.ofHours(8))
      }
    }
  }

  @Test
  fun `DATES-NWD-02 Week Frequency Skips Non Working Days`() {
    schedulingScenario {
      test("DATES-NWD-02")
      given {
        today(MONDAY)
        project("PROJ1")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(WEEKLY)
          on(MONDAY)
          from("10:00")
          until("18:00")
        }

        nonWorkingDate(7)
        nonWorkingDate(14)
      }

      whenScheduling {
        requirementIsHours(32)
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
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(35)
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
      test("DATES-NWD-03")
      given {
        today(MONDAY)
        project("PROJ1")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(FORTNIGHTLY)
          on(MONDAY)
          from("10:00")
          until("18:00")
          startingToday()
        }

        nonWorkingDate(14)
      }

      whenScheduling {
        requirementIsHours(32)
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
            project("PROJ1")
            allocation("ALLOC1")
            today(28)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(42)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(56)
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
      test("DATES-NWD-04")
      given {
        today(MONDAY)
        project("PROJ1")

        allocation {
          id("ALLOC1")
          project("PROJ1")
          frequency(WEEKLY)
          on(MONDAY)
          from("10:00")
          until("18:00")
        }

        nonWorkingDate(0)
      }

      whenScheduling {
        requirementIsHours(32)
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(7)
            from("10:00")
            until("18:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(14)
            from("10:00")
            until("18:00")
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
      }
    }
  }
}

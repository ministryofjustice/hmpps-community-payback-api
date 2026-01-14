package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/**
 * These scenarios will check that the remaining time is correctly calculated by checking
 * appointments are created that are sufficient to meet this remaining time
 *
 * They will also ensure that the final appointment end time is truncated where necessary to ensure
 * no more minutes than required are scheduled
 */
class SchedulingScenariosRemainingTimeTest {

  @Test
  fun `REMAINING-TIME-01 0 Requirement`() {
    schedulingScenario {
      test("REMAINING-TIME-01")
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
      }

      whenScheduling {
        requirementIs(Duration.ZERO)
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `REMAINING-TIME-02 No Scheduled Appointments, Create 1 non-truncated Appointment Today`() {
    schedulingScenario {
      test("REMAINING-TIME-02")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }
      }

      whenScheduling {
        requirementIsHours(8)
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
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-03 No Scheduled Appointments, Create 1 truncated Appointment Today`() {
    schedulingScenario {
      test("REMAINING-TIME-03")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }
      }

      whenScheduling {
        requirementIsHours(4)
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("14:00")
          }
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-04 Pending Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    schedulingScenario {
      test("REMAINING-TIME-04")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("16:00")
          until("20:00")
          pending()
        }
      }

      whenScheduling {
        requirementIsHours(12)
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
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-05 Credited Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    schedulingScenario {
      test("REMAINING-TIME-05")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("16:00")
          until("20:00")
          credited(Duration.ofHours(4))
        }
      }

      whenScheduling {
        requirementIsHours(12)
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
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-06 Non Attended Past Appointment Insufficient, Create 1 non-truncated Appointment Today`() {
    schedulingScenario {
      test("REMAINING-TIME-06")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("16:00")
          until("20:00")
          nonAttended()
        }
      }

      whenScheduling {
        requirementIsHours(8)
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
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-07 Pending Past Appointment Insufficient, Create 1 truncated Appointment`() {
    schedulingScenario {
      test("REMAINING-TIME-07")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("10:00")
          until("14:00")
          pending()
        }
      }

      whenScheduling {
        requirementIs(Duration.parse("PT10H30M"))
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("16:30")
          }
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-08 Credited Past Appointment Insufficient, Create 1 truncated Appointment`() {
    schedulingScenario {
      test("REMAINING-TIME-08")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("10:00")
          until("14:00")
          credited(Duration.ofHours(4))
        }
      }

      whenScheduling {
        requirementIs(Duration.ofHours(11))
      }

      then {
        shouldCreateAppointments {
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("17:00")
          }
        }
      }
    }
  }

  @Test
  fun `REMAINING-TIME-09 Credited Past Appointments Insufficient, Create multiple Appointments including truncated final Appointment`() {
    schedulingScenario {
      test("REMAINING-TIME-09")
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
        }

        allocation {
          id("ALLOC2")
          project("PROJ2")
          frequency(WEEKLY)
          on(TUESDAY)
          from("16:00")
          until("20:00")
        }

        appointment {
          project("PROJ1")
          allocation("ALLOC1")
          today(-14)
          from("10:00")
          until("18:00")
          credited(Duration.parse("PT3H30M"))
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-13)
          from("10:00")
          until("14:00")
          nonAttended()
        }

        appointment {
          project("PROJ1")
          allocation("ALLOC1")
          today(-7)
          from("10:00")
          until("18:00")
          nonAttended()
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(-6)
          from("10:00")
          until("14:00")
          credited(Duration.parse("PT4H"))
        }

        appointment {
          project("PROJ1")
          allocation("ALLOC1")
          today()
          from("10:00")
          until("18:00")
          pending()
        }

        appointment {
          project("PROJ2")
          allocation("ALLOC2")
          today(1)
          from("16:00")
          until("20:00")
          pending()
        }
      }

      whenScheduling {
        requirementIs(Duration.parse("PT44H"))
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
            project("PROJ2")
            allocation("ALLOC2")
            today(8)
            from("16:00")
            until("20:00")
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
            today(15)
            from("16:00")
            until("20:00")
          }
          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(21)
            from("10:00")
            until("10:30")
          }
        }
      }
    }
  }
}

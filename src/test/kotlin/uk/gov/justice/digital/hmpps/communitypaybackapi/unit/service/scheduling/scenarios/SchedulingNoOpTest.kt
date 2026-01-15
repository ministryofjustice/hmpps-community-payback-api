package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration

/**
 * Existing appointments already satisfy requirements (No-op)
 *
 * Once deleting appointments is in scope, scenarios with a surplus should lead to appointments being deleted
 */
class SchedulingNoOpTest {

  @Test
  fun `NOOP-01 Today's Pending Appointment Satisfies Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-01")
      given {
        requirementIsHours(8)
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("12:00")
          until("20:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today()
          from("12:00")
          until("20:00")
          pending()
        }
      }

      then {
        existingAppointmentsSufficient()
      }
    }
  }

  @Test
  fun `NOOP-02 Today's Completed Appointment Time Credited Satisfies Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-02")
      given {
        requirementIs(Duration.parse("PT6H30M"))
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("12:00")
          until("20:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today()
          from("12:00")
          until("20:00")
          credited(Duration.parse("PT6H30M"))
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-03 Today's Pending Appointment Exceeds Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-03")
      given {
        requirementIs(Duration.parse("PT6H30M"))
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("20:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today()
          from("10:00")
          until("16:30")
          pending()
        }
      }

      then {
        existingAppointmentsSufficient()
      }
    }
  }

  @Test
  fun `NOOP-04 Today's Complete Appointment Satisfies Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-04")
      given {
        requirementIs(Duration.parse("PT6H30M"))
        todayIs(MONDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("20:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today()
          from("10:00")
          until("18:00")
          credited(Duration.parse("PT8H"))
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-05 Yesterday's Pending Appointment Satisfies Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-05")
      given {
        requirementIs(Duration.parse("PT6H30M"))
        todayIs(TUESDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(-1)
          from("10:00")
          until("18:30")
          pending()
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-06 Yesterday's Complete Appointment Satisfies Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-06")
      given {
        requirementIs(Duration.parse("PT2H"))
        todayIs(TUESDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(-1)
          from("10:00")
          until("18:00")
          credited(Duration.parse("PT2H"))
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-07 Multiple Complete and Pending Past Appointments Satisfy Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-07")
      given {
        requirementIs(Duration.parse("PT120H"))
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

        listOf(-7, -14, -21, -28, -35, -42, -49, -56, -63, -70, -77, -84, -91, -98, -105).forEach { offset ->
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today(offset)
            from("10:00")
            until("18:00")
            credited(Duration.parse("PT8H"))
          }
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-08 1 Past and 1 Future Pending Appointments Satisfy Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-08")
      given {
        requirementIs(Duration.parse("PT8H"))
        todayIs(WEDNESDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(TUESDAY)
          from("08:00")
          until("12:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(-1)
          from("08:00")
          until("12:00")
          pending()
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(6)
          from("08:00")
          until("12:00")
          pending()
        }
      }

      then {
        existingAppointmentsSufficient()
      }
    }
  }

  @Test
  fun `NOOP-09 1 Past and 1 Future Complete Appointments Satisfy Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-09")
      given {
        requirementIs(Duration.ofHours(8))
        todayIs(WEDNESDAY)
        projectExistsWithCode("PROJ1")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(TUESDAY)
          from("08:00")
          until("12:00")
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(-1)
          from("08:00")
          until("12:00")
          credited(Duration.ofHours(4))
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(6)
          from("08:00")
          until("12:00")
          credited(Duration.ofHours(4))
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }

  @Test
  fun `NOOP-10 Many Past and Future Pending and Complete Appointments Across Multiple Allocations Satisfy Requirement`() {
    schedulingScenario {
      scenarioId("NOOP-10")
      given {
        requirementIs(Duration.parse("PT46H"))
        todayIs(WEDNESDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("12:00")
          until("20:00")
        }

        allocation {
          alias("ALLOC2")
          projectCode("PROJ2")
          frequency(WEEKLY)
          onWeekDay(TUESDAY)
          from("10:00")
          until("14:00")
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today(-50)
          from("02:00")
          until("06:00")
          credited(Duration.parse("PT4H"))
        }

        appointment {
          projectCode("PROJ1")
          allocation("ALLOC1")
          today(-2)
          from("12:00")
          until("18:00")
          credited(Duration.parse("PT6H"))
        }

        appointment {
          projectCode("PROJ2")
          allocation("ALLOC2")
          today(-1)
          from("10:00")
          until("14:00")
          credited(Duration.parse("PT0H"))
        }

        listOf(5, 12, 19).forEach { offset ->
          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today(offset)
            from("12:00")
            until("20:00")
            pending()
          }
        }

        listOf(6, 13, 20).forEach { offset ->
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            today(offset)
            from("10:00")
            until("14:00")
            pending()
          }
        }
      }

      then {
        existingAppointmentsSufficient()
      }
    }
  }

  @Test
  fun `NOOP-11 Surplus Appointments are not removed`() {
    schedulingScenario {
      scenarioId("NOOP-11")
      given {
        requirementIs(Duration.ofHours(8))
        todayIs(WEDNESDAY)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("12:00")
          until("20:00")
        }

        allocation {
          alias("ALLOC2")
          projectCode("PROJ2")
          frequency(WEEKLY)
          onWeekDay(TUESDAY)
          from("10:00")
          until("14:00")
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today(-20)
          from("00:00")
          until("04:00")
          credited(Duration.parse("PT4H"))
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today(-10)
          from("00:30")
          until("04:30")
          credited(Duration.parse("PT4H"))
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today()
          from("01:00")
          until("09:00")
          pending()
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today(10)
          from("01:30")
          until("09:30")
          pending()
        }

        appointment {
          projectCode("PROJ1")
          manual()
          today(20)
          from("02:00")
          until("10:00")
          pending()
        }
      }

      then {
        requirementAlreadySatisfied()
      }
    }
  }
}

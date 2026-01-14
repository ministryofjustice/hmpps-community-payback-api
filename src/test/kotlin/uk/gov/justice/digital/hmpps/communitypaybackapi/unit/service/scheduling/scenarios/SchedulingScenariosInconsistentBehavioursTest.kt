package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.ONCE
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.Duration

/**
 * These scenarios highlight inconsistent behaviour that we've emulated
 * from the NDelius implementation
 */
class SchedulingScenariosInconsistentBehavioursTest {

  @Nested
  inner class AllocationClashesDoubleBookings {

    @Test
    fun `INC-CLASH-01 Double Bookings are made if double booked allocations exist and there are no existing appointments on that date`() {
      schedulingScenario {
        scenarioId("INC-CLASH-01")
        given {
          requirementIsHours(44)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ3")
          projectExistsWithCode("PROJ4")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingInDays(7)
            endingInDays(7)
          }

          allocation {
            alias("ALLOC2")
            projectCode("PROJ2")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("12:00")
            until("20:00")
          }

          allocation {
            alias("ALLOC3")
            projectCode("PROJ3")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
          }

          allocation {
            alias("ALLOC4")
            projectCode("PROJ4")
            frequency(FORTNIGHTLY)
            onWeekDay(MONDAY)
            from("06:00")
            until("14:00")
            startingInDays(-7)
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays()
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays()
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(7)
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ4")
              allocation("ALLOC4")
              todayWithOffsetDays(7)
              from("06:00")
              until("14:00")
            }
          }
        }
      }
    }

    @Test
    fun `INC-CLASH-02 Double Bookings are not made if double booked allocations exist and there is at least one appointment on the date already, has outcome`() {
      schedulingScenario {
        scenarioId("INC-CLASH-02")
        given {
          requirementIsHours(58)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ3")
          projectExistsWithCode("PROJ4")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingInDays(7)
            endingInDays(7)
          }

          allocation {
            alias("ALLOC2")
            projectCode("PROJ2")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("12:00")
            until("20:00")
          }

          allocation {
            alias("ALLOC3")
            projectCode("PROJ3")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
          }

          appointment {
            projectCode("PROJ4")
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT6H"))
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(7)
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(14)
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(21)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(21)
              from("10:00")
              until("18:00")
            }
          }
        }
      }
    }

    @Test
    fun `INC-CLASH-03 Double Bookings are not made if double booked allocations exist and there is at least one appointment on the date already, pending`() {
      schedulingScenario {
        scenarioId("INC-CLASH-03")
        given {
          requirementIsHours(30)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ4")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingInDays(7)
            endingInDays(7)
          }

          allocation {
            alias("ALLOC2")
            projectCode("PROJ2")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("12:00")
            until("20:00")
          }

          appointment {
            projectCode("PROJ4")
            manual()
            today()
            from("12:00")
            until("20:00")
            pending()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(21)
              from("12:00")
              until("14:00")
            }
          }
        }
      }
    }
  }

  @Nested
  inner class ManualAppointmentsAndScheduling {

    @Test
    fun `INC-MANUAL-01 Manually created appointments in the future without an outcome are retained by the scheduler if attempting to allocate to same day`() {
      schedulingScenario {
        scenarioId("INC-MANUAL-01")
        given {
          requirementIsHours(24)
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
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }

          appointment {
            projectCode("PROJ1")
            manual()
            today(7)
            from("12:00")
            until("13:00")
            pending()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(21)
              from("12:00")
              until("19:00")
            }
          }
        }
      }
    }

    @Test
    fun `INC-MANUAL-02 Appointments in the future are retained but potential time credited ignored if not attempting to allocate to same day`() {
      schedulingScenario {
        scenarioId("INC-MANUAL-02")
        given {
          requirementIsHours(16)
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
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }

          appointment {
            projectCode("PROJ1")
            manual()
            today(1)
            from("00:00")
            until("23:00")
            pending()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }
  }
}

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
        test("INC-CLASH-01")
        given {
          today(MONDAY)
          project("PROJ1")
          project("PROJ2")
          project("PROJ3")
          project("PROJ4")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("14:00")
            startingIn(7)
            endingIn(7)
          }

          allocation {
            id("ALLOC2")
            project("PROJ2")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("20:00")
          }

          allocation {
            id("ALLOC3")
            project("PROJ3")
            frequency(WEEKLY)
            on(MONDAY)
            from("10:00")
            until("18:00")
          }

          allocation {
            id("ALLOC4")
            project("PROJ4")
            frequency(FORTNIGHTLY)
            on(MONDAY)
            from("06:00")
            until("14:00")
            startingIn(-7)
          }
        }

        whenScheduling {
          requirementIsHours(44)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today()
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today()
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(7)
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ4")
              allocation("ALLOC4")
              today(7)
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
        test("INC-CLASH-02")
        given {
          today(MONDAY)
          project("PROJ1")
          project("PROJ2")
          project("PROJ3")
          project("PROJ4")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("14:00")
            startingIn(7)
            endingIn(7)
          }

          allocation {
            id("ALLOC2")
            project("PROJ2")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("20:00")
          }

          allocation {
            id("ALLOC3")
            project("PROJ3")
            frequency(WEEKLY)
            on(MONDAY)
            from("10:00")
            until("18:00")
          }

          appointment {
            project("PROJ4")
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT6H"))
          }
        }

        whenScheduling {
          requirementIsHours(58)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(7)
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(14)
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(21)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(21)
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
        test("INC-CLASH-03")
        given {
          today(MONDAY)
          project("PROJ1")
          project("PROJ2")
          project("PROJ4")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("14:00")
            startingIn(7)
            endingIn(7)
          }

          allocation {
            id("ALLOC2")
            project("PROJ2")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("20:00")
          }

          appointment {
            project("PROJ4")
            manual()
            today()
            from("12:00")
            until("20:00")
            pending()
          }
        }

        whenScheduling {
          requirementIsHours(30)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(7)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(21)
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
        test("INC-MANUAL-01")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("20:00")
          }

          appointment {
            project("PROJ1")
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }

          appointment {
            project("PROJ1")
            manual()
            today(7)
            from("12:00")
            until("13:00")
            pending()
          }
        }

        whenScheduling {
          requirementIsHours(24)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(14)
              from("12:00")
              until("20:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(21)
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
        test("INC-MANUAL-02")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("20:00")
          }

          appointment {
            project("PROJ1")
            manual()
            today()
            from("12:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }

          appointment {
            project("PROJ1")
            manual()
            today(1)
            from("00:00")
            until("23:00")
            pending()
          }
        }

        whenScheduling {
          requirementIsHours(16)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }
  }
}

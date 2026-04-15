package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.ONCE
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.Duration

/**
 * Whilst we have not fully implemented scheduling on allocation changes, these unit tests have been
 * added to provide a counter example to tests defined in [SchedulingApptChangeTriggerBehavioursTest]
 */
class SchedulingAllocChangeTriggerBehavioursTest {

  @Nested
  inner class AllocationClashesDoubleBookings {

    @Test
    fun `Double Bookings are made if double booked allocations exist and there are no existing appointments on that date`() {
      schedulingScenario {
        scenarioId("NO-ID")
        given {
          requirementHoursAre(44)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ3")
          projectExistsWithCode("PROJ4")
          schedulingIsTriggeredByAnAllocationChange()

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
    fun `Double Bookings are made if double booked allocations exist and there is at least one appointment on the date already, has outcome`() {
      schedulingScenario {
        scenarioId("NO-ID")
        given {
          requirementHoursAre(58)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ3")
          projectExistsWithCode("PROJ4")
          schedulingIsTriggeredByAnAllocationChange()

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
          }
        }
      }
    }

    @Test
    fun `Double Bookings are not made if appointment on same day for same allocation has allocation start and end time, has no outcome`() {
      schedulingScenario {
        scenarioId("NO-ID")
        given {
          requirementHoursAre(4)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          schedulingIsTriggeredByAnAllocationChange()

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingToday()
            endingInDays(7)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("14:00")
            pending()
          }
        }

        then {
          existingAppointmentsSufficient()
        }
      }
    }

    @Test
    fun `Double Bookings are not made if appointment on same day for same allocation has allocation start and end time, has outcome`() {
      schedulingScenario {
        scenarioId("NO-ID")
        given {
          requirementHoursAre(4)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          schedulingIsTriggeredByAnAllocationChange()

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingToday()
            endingInDays(7)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("14:00")
          }
        }

        then {
          existingAppointmentsSufficient()
        }
      }
    }

    /*
    This highlights a bug in our current implementation because it differs from the NDelius implementation
    because "This doesn’t happen if there is at least one existing appointment today for the allocation that has an outcome recorded"
     */
    @Test
    fun `Double Bookings are made if appointment on same day for same allocation has different allocation start and end time, has outcome`() {
      schedulingScenario {
        scenarioId("NO-ID")
        given {
          requirementHoursAre(4)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")
          schedulingIsTriggeredByAnAllocationChange()

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("14:00")
            startingToday()
            endingInDays(7)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today()
            from("10:00")
            until("12:00")
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("14:00")
            }
          }
        }
      }
    }
  }
}

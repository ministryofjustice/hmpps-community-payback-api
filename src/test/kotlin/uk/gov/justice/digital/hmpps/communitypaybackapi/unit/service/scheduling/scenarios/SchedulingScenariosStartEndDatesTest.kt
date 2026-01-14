package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

class SchedulingScenariosStartEndDatesTest {

  @Nested
  inner class StartDate {

    @Test
    fun `DATES-START-01 Allocation Start Date is tomorrow`() {
      schedulingScenario {
        test("DATES-START-01")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(TUESDAY)
            from("10:00")
            until("18:00")
            startingIn(1)
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
              today(1)
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(8)
              from("10:00")
              until("18:00")
            }
          }
        }
      }
    }

    @Test
    fun `DATES-START-02 Allocation Start Date is today`() {
      schedulingScenario {
        test("DATES-START-02")
        given {
          today(TUESDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(TUESDAY)
            from("10:00")
            until("18:00")
            startingToday()
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
              today()
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("10:00")
              until("18:00")
            }
          }
        }
      }
    }

    @Test
    fun `DATES-START-03 Allocation Start Date is in far future`() {
      schedulingScenario {
        test("DATES-START-03")
        given {
          today(TUESDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(TUESDAY)
            from("10:00")
            until("18:00")
            startingIn(700)
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
              today(700)
              from("10:00")
              until("18:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(707)
              from("10:00")
              until("18:00")
            }
          }
        }
      }
    }
  }

  @Nested
  inner class EndDates {

    @Test
    fun `DATES-END-01 Allocation End Date is day of next iteration`() {
      schedulingScenario {
        test("DATES-END-01")
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
            endingIn(14)
          }
        }

        whenScheduling {
          requirementIsHours(80)
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
              today(14)
              from("10:00")
              until("18:00")
            }
          }
          withShortfall(Duration.ofHours(64))
        }
      }
    }

    @Test
    fun `DATES-END-02 Allocation End Date is day before next iteration`() {
      schedulingScenario {
        test("DATES-END-02")
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
            endingIn(13)
          }
        }

        whenScheduling {
          requirementIsHours(80)
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
          withShortfall(Duration.ofHours(72))
        }
      }
    }

    @Disabled
    fun `DATES-END-03 Allocation Start Date is same as End Date`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // these allocations are filtered out when mapping the data models
    }

    @Test
    fun `DATES-END-04 Allocation End Date is so close to Start Date it prohibits Appointments being created`() {
      schedulingScenario {
        test("DATES-END-04")
        given {
          today(TUESDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(MONDAY)
            from("10:00")
            until("18:00")
            startingIn(-1)
            endingIn(12)
          }
        }

        whenScheduling {
          requirementIsHours(80)
        }

        then {
          shouldCreateAppointments { }
          withShortfall(Duration.ofHours(80))
        }
      }
    }

    @Test
    fun `DATES-END-05 Allocation ends in the past`() {
      schedulingScenario {
        test("DATES-END-05")
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
            startingIn(-365)
            endingIn(-1)
          }
        }

        whenScheduling {
          requirementIsHours(80)
        }

        then {
          shouldCreateAppointments { }
          withShortfall(Duration.ofHours(80))
        }
      }
    }

    @Disabled
    fun `DATES-END-06 Allocation end date not defined and project's expected end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }

    @Disabled
    fun `DATES-END-07 Allocation end date not defined and project's actual end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }

    @Disabled
    fun `DATES-END-08 Allocation end date not defined and availability's end date is earliest`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which will ensure
      // the correct end date is used when building the allocation data model
    }
  }
}

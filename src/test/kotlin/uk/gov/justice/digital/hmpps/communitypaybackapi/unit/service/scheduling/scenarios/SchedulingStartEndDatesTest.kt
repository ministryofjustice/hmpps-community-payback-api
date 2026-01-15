package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

class SchedulingStartEndDatesTest {

  @Nested
  inner class StartDate {

    @Test
    fun `DATES-START-01 Allocation Start Date is tomorrow`() {
      schedulingScenario {
        scenarioId("DATES-START-01")
        given {
          requirementIsHours(16)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(TUESDAY)
            from("10:00")
            until("18:00")
            startingInDays(1)
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(1)
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(8)
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
        scenarioId("DATES-START-02")
        given {
          requirementIsHours(16)
          todayIs(TUESDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(TUESDAY)
            from("10:00")
            until("18:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
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
        scenarioId("DATES-START-03")
        given {
          requirementIsHours(16)
          todayIs(TUESDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(TUESDAY)
            from("10:00")
            until("18:00")
            startingInDays(700)
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(700)
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(707)
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
        scenarioId("DATES-END-01")
        given {
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            requirementIsHours(80)
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
            startingToday()
            endingInDays(14)
          }
        }

        then {
          shouldCreateAppointments(toAddressShortfall = Duration.ofHours(64)) {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("18:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(14)
              from("10:00")
              until("18:00")
            }
          }
        }
      }
    }

    @Test
    fun `DATES-END-02 Allocation End Date is day before next iteration`() {
      schedulingScenario {
        scenarioId("DATES-END-02")
        given {
          requirementIsHours(80)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
            startingToday()
            endingInDays(13)
          }
        }

        then {
          shouldCreateAppointments(toAddressShortfall = Duration.ofHours(72)) {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("18:00")
            }
          }
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
        scenarioId("DATES-END-04")
        given {
          requirementIsHours(80)
          todayIs(TUESDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
            startingInDays(-1)
            endingInDays(12)
          }
        }

        then {
          noActionsExpected(toAddressShortfall = Duration.ofHours(80))
        }
      }
    }

    @Test
    fun `DATES-END-05 Allocation ends in the past`() {
      schedulingScenario {
        scenarioId("DATES-END-05")
        given {
          requirementIsHours(80)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(MONDAY)
            from("10:00")
            until("18:00")
            startingInDays(-365)
            endingInDays(-1)
          }
        }

        then {
          noActionsExpected(toAddressShortfall = Duration.ofHours(80))
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

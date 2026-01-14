package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.FORTNIGHTLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.ONCE
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency.WEEKLY
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/**
 * These Scenarios test the Allocation Frequencies are correctly applied
 */
class SchedulingScenariosFrequencyTest {

  /**
   * Once has some surprising/inconsistent behaviour. These are modelled by the ‘inconsistent behaviour’ scenarios
   */
  @Nested
  inner class Once {

    @Test
    fun `FREQ-ONCE-01 Schedule 'Once' Allocation for today`() {
      schedulingScenario {
        test("FREQ-ONCE-01")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("12:00")
            until("20:00")
            startingToday()
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
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-02 Schedule 'Once' Allocation tomorrow`() {
      schedulingScenario {
        test("FREQ-ONCE-02")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(TUESDAY)
            from("12:00")
            until("20:00")
            startingToday()
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
              today(1)
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-03 Schedule 'Once' Allocation in far future`() {
      schedulingScenario {
        test("FREQ-ONCE-03")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("12:00")
            until("20:00")
            startingIn(700)
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
              today(700)
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-04 Schedule 'Once' Allocation once`() {
      schedulingScenario {
        test("FREQ-ONCE-04")
        given {
          today(THURSDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(FRIDAY)
            from("12:00")
            until("20:00")
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
              from("12:00")
              until("20:00")
            }
          }
          withShortfall(Duration.ofHours(8))
        }
      }
    }

    @Test
    fun `FREQ-ONCE-05 'Once' allocation already scheduled last week will result in multiple appointments if end date allows`() {
      schedulingScenario {
        test("FREQ-ONCE-05")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("20:00")
            startingIn(-7)
            endingIn(1)
          }

          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(-7)
            from("10:00")
            until("20:00")
            credited(Duration.ofHours(8))
          }
        }

        whenScheduling {
          requirementIsHours(40)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today()
              from("10:00")
              until("20:00")
            }
          }
          withShortfall(Duration.ofHours(22))
        }
      }
    }

    @Test
    fun `FREQ-ONCE-06 'Once' allocation already scheduled months ago will result in multiple appointments if end date allows`() {
      schedulingScenario {
        test("FREQ-ONCE-06")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("20:00")
            startingIn(-365)
            endingIn(1)
          }

          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(-365)
            from("10:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }
        }

        whenScheduling {
          requirementIsHours(40)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today()
              from("10:00")
              until("20:00")
            }
          }
          withShortfall(Duration.ofHours(22))
        }
      }
    }

    @Test
    fun `FREQ-ONCE-07 'Once' allocation with suitable end date will not result in multiple appointments`() {
      schedulingScenario {
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("20:00")
            startingIn(-7)
            endingIn(-1)
          }

          appointment {
            project("PROJ1")
            allocation("ALLOC1")
            today(-7)
            from("10:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }
        }

        whenScheduling {
          requirementIsHours(40)
        }

        then {
          shouldCreateAppointments { }
          withShortfall(Duration.ofHours(32))
        }
      }
    }
  }

  @Nested
  inner class Weekly {

    @CsvSource(
      "MONDAY,0",
      "TUESDAY,1",
      "WEDNESDAY,2",
      "THURSDAY,3",
      "FRIDAY,4",
      "SATURDAY,5",
      "SUNDAY,6",
    )
    @ParameterizedTest
    fun `FREQ-WK-01 Schedule Weekly Allocation on the Correct Day`(day: DayOfWeek, offset: Int) {
      schedulingScenario {
        test("FREQ-WK-01")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(day)
            from("10:00")
            until("14:00")
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
              today(offset)
              from("10:00")
              until("14:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-WK-02 Schedule Weekly Allocation until requirement met, Allocation starting yesterday`() {
      schedulingScenario {
        test("FREQ-WK-02")
        given {
          today(TUESDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(MONDAY)
            from("12:00")
            until("16:30")
          }
        }

        whenScheduling {
          requirementIsHours(27)
        }

        then {
          shouldCreateAppointments {
            listOf(6, 13, 20, 27, 34, 41).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("12:00")
                until("16:30")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-WK-03 Schedule Weekly Allocation until requirement met, Allocation starting today`() {
      schedulingScenario {
        test("FREQ-WK-03")
        given {
          today(TUESDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(TUESDAY)
            from("12:00")
            until("22:00")
          }
        }

        whenScheduling {
          requirementIsHours(50)
        }

        then {
          shouldCreateAppointments {
            listOf(0, 7, 14, 21, 28).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("12:00")
                until("22:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-WK-04 Schedule Weekly Allocation until requirement met, Allocation starting tomorrow`() {
      schedulingScenario {
        test("FREQ-WK-04")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(TUESDAY)
            from("00:00")
            until("10:00")
          }
        }

        whenScheduling {
          requirementIsHours(80)
        }

        then {
          shouldCreateAppointments {
            listOf(1, 8, 15, 22, 29, 36, 43, 50).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("00:00")
                until("10:00")
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class Fortnightly {

    @CsvSource(
      "MONDAY,0,14",
      "TUESDAY,1,15",
      "WEDNESDAY,2,16",
      "THURSDAY,3,17",
      "FRIDAY,4,18",
      "SATURDAY,5,19",
      "SUNDAY,6,20",
    )
    @ParameterizedTest
    fun `FREQ-FN-01 Schedule Fortnightly Allocation on the Correct Day`(day: DayOfWeek, offset1: Int, offset2: Int) {
      schedulingScenario {
        test("FREQ-FN-01")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(day)
            from("10:00")
            until("14:00")
            startingToday()
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
              today(offset1)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(offset2)
              from("10:00")
              until("14:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-02 Schedule Fortnightly Allocation until requirement met, iterating from Allocation start date`() {
      schedulingScenario {
        test("FREQ-FN-02")
        given {
          today(MONDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(DayOfWeek.SUNDAY)
            from("02:00")
            until("04:00")
            startingIn(-705)
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(13, 27, 41, 55).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("02:00")
                until("04:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-03 Schedule Fortnightly Allocation until requirement met, starting 14 days ago`() {
      schedulingScenario {
        test("FREQ-FN-03")
        given {
          today(FRIDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(FRIDAY)
            from("20:30")
            until("22:30")
            startingIn(-14)
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(0, 14, 28, 42).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("20:30")
                until("22:30")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-04 Schedule Fortnightly Allocation until requirement met, starting 8 days ago`() {
      schedulingScenario {
        test("FREQ-FN-04")
        given {
          today(SATURDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(FRIDAY)
            from("00:00")
            until("02:00")
            startingIn(-8)
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(6, 20, 34, 48).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("00:00")
                until("02:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-05 Schedule Fortnightly Allocation until requirement met, starting yesterday`() {
      schedulingScenario {
        test("FREQ-FN-05")
        given {
          today(FRIDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(THURSDAY)
            from("11:00")
            until("13:00")
            startingIn(-1)
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(13, 27, 41, 55).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("11:00")
                until("13:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-06 Schedule Fortnightly Allocation until requirement met, starting today`() {
      schedulingScenario {
        test("FREQ-FN-06")
        given {
          today(FRIDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(FRIDAY)
            from("12:00")
            until("14:00")
            startingToday()
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(0, 14, 28, 42).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("12:00")
                until("14:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-07 Schedule Fortnightly Allocation until requirement met, starting tomorrow`() {
      schedulingScenario {
        test("FREQ-FN-07")
        given {
          today(SATURDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(DayOfWeek.SUNDAY)
            from("13:00")
            until("15:00")
            startingIn(1)
          }
        }

        whenScheduling {
          requirementIsHours(8)
        }

        then {
          shouldCreateAppointments {
            listOf(1, 15, 29, 43).forEach { offset ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(offset)
                from("13:00")
                until("15:00")
              }
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-FN-08 Schedule Fortnightly Allocation until requirement met, over a year`() {
      schedulingScenario {
        test("FREQ-FN-08")
        given {
          today(FRIDAY)
          project("PROJ1")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(FORTNIGHTLY)
            on(FRIDAY)
            from("14:00")
            until("15:00")
            startingToday()
          }
        }

        whenScheduling {
          requirementIsHours(52)
        }

        then {
          shouldCreateAppointments {
            (0..51).forEach { weeks ->
              appointment {
                project("PROJ1")
                allocation("ALLOC1")
                today(weeks * 14)
                from("14:00")
                until("15:00")
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class Mixed {

    @Test
    fun `FREQ-MIXED-01 Multiple Allocations of different Frequencies`() {
      schedulingScenario {
        test("FREQ-MIXED-01")
        given {
          today(FRIDAY)
          project("PROJ1")
          project("PROJ2")
          project("PROJ3")
          project("PROJ4")
          project("PROJ5")
          project("PROJ6")

          allocation {
            id("ALLOC1")
            project("PROJ1")
            frequency(WEEKLY)
            on(FRIDAY)
            from("10:00")
            until("12:00")
            startingToday()
          }

          allocation {
            id("ALLOC2")
            project("PROJ2")
            frequency(FORTNIGHTLY)
            on(SATURDAY)
            from("10:00")
            until("13:00")
            startingToday()
          }

          allocation {
            id("ALLOC3")
            project("PROJ3")
            frequency(WEEKLY)
            on(DayOfWeek.SUNDAY)
            from("10:00")
            until("14:00")
            startingToday()
          }

          allocation {
            id("ALLOC4")
            project("PROJ4")
            frequency(ONCE)
            on(MONDAY)
            from("10:00")
            until("15:00")
            startingIn(3)
          }

          allocation {
            id("ALLOC5")
            project("PROJ5")
            frequency(FORTNIGHTLY)
            on(THURSDAY)
            from("10:00")
            until("16:00")
            startingIn(13)
          }

          allocation {
            id("ALLOC6")
            project("PROJ6")
            frequency(ONCE)
            on(DayOfWeek.WEDNESDAY)
            from("10:00")
            until("17:00")
            startingIn(22)
          }
        }

        whenScheduling {
          requirementIsHours(37)
        }

        then {
          shouldCreateAppointments {
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today()
              from("10:00")
              until("12:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(1)
              from("10:00")
              until("13:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(2)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ4")
              allocation("ALLOC4")
              today(3)
              from("10:00")
              until("15:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(7)
              from("10:00")
              until("12:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(9)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ5")
              allocation("ALLOC5")
              today(13)
              from("10:00")
              until("16:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(14)
              from("10:00")
              until("12:00")
            }
            appointment {
              project("PROJ2")
              allocation("ALLOC2")
              today(15)
              from("10:00")
              until("13:00")
            }
            appointment {
              project("PROJ3")
              allocation("ALLOC3")
              today(16)
              from("10:00")
              until("14:00")
            }
            appointment {
              project("PROJ1")
              allocation("ALLOC1")
              today(21)
              from("10:00")
              until("12:00")
            }
          }
        }
      }
    }

    @Disabled
    fun `FREQ-MIXED-02 Use largest frequency defined between Allocation and linked Availability`() {
      // This scenario is implicitly tested by the [SchedulingMappersTest] which ensures the
      // largest frequency is used when building the internal allocation models
    }
  }
}

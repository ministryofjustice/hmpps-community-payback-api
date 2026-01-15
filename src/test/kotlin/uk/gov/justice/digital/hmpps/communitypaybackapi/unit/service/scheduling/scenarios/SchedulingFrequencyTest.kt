package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

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
class SchedulingFrequencyTest {

  /**
   * Once has some surprising/inconsistent behaviour. These are modelled by the ‘inconsistent behaviour’ scenarios
   */
  @Nested
  inner class Once {

    @Test
    fun `FREQ-ONCE-01 Schedule 'Once' Allocation for today`() {
      schedulingScenario {
        scenarioId("FREQ-ONCE-01")
        given {
          requirementIsHours(8)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("12:00")
            until("20:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
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
        scenarioId("FREQ-ONCE-02")
        given {
          requirementIsHours(8)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(TUESDAY)
            from("12:00")
            until("20:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(1)
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
        scenarioId("FREQ-ONCE-03")
        given {
          requirementIsHours(8)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("12:00")
            until("20:00")
            startingInDays(700)
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(700)
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
        scenarioId("FREQ-ONCE-04")
        given {
          requirementIsHours(16)
          todayIs(THURSDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(FRIDAY)
            from("12:00")
            until("20:00")
            startingInDays(1)
          }
        }

        then {
          shouldCreateAppointments(toAddressShortfall = Duration.ofHours(8)) {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(1)
              from("12:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-05 'Once' allocation already scheduled last week will result in multiple appointments if end date allows`() {
      schedulingScenario {
        scenarioId("FREQ-ONCE-05")
        given {
          requirementIsHours(40)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("20:00")
            startingInDays(-7)
            endingInDays(1)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today(-7)
            from("10:00")
            until("20:00")
            credited(Duration.ofHours(8))
          }
        }

        then {
          shouldCreateAppointments(toAddressShortfall = Duration.ofHours(22)) {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-06 'Once' allocation already scheduled months ago will result in multiple appointments if end date allows`() {
      schedulingScenario {
        scenarioId("FREQ-ONCE-06")
        given {
          requirementIsHours(40)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("20:00")
            startingInDays(-365)
            endingInDays(1)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today(-365)
            from("10:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }
        }

        then {
          shouldCreateAppointments(toAddressShortfall = Duration.ofHours(22)) {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("20:00")
            }
          }
        }
      }
    }

    @Test
    fun `FREQ-ONCE-07 'Once' allocation with suitable end date will not result in multiple appointments`() {
      schedulingScenario {
        given {
          requirementIsHours(40)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("20:00")
            startingInDays(-7)
            endingInDays(-1)
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            today(-7)
            from("10:00")
            until("20:00")
            credited(Duration.parse("PT8H"))
          }
        }

        then {
          noActionsExpected(toAddressShortfall = Duration.ofHours(32))
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
        scenarioId("FREQ-WK-01")
        given {
          requirementIsHours(4)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(day)
            from("10:00")
            until("14:00")
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(offset)
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
        scenarioId("FREQ-WK-02")
        given {
          requirementIsHours(27)
          todayIs(TUESDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(MONDAY)
            from("12:00")
            until("16:30")
          }
        }

        then {
          shouldCreateAppointments {
            listOf(6, 13, 20, 27, 34, 41).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-WK-03")
        given {
          requirementIsHours(50)
          todayIs(TUESDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(TUESDAY)
            from("12:00")
            until("22:00")
          }
        }

        then {
          shouldCreateAppointments {
            listOf(0, 7, 14, 21, 28).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-WK-04")
        given {
          requirementIsHours(80)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(TUESDAY)
            from("00:00")
            until("10:00")
          }
        }

        then {
          shouldCreateAppointments {
            listOf(1, 8, 15, 22, 29, 36, 43, 50).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-01")
        given {
          requirementIsHours(8)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(day)
            from("10:00")
            until("14:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(offset1)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(offset2)
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
        scenarioId("FREQ-FN-02")
        given {
          requirementIsHours(8)
          todayIs(MONDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(DayOfWeek.SUNDAY)
            from("02:00")
            until("04:00")
            startingInDays(-705)
          }
        }

        then {
          shouldCreateAppointments {
            listOf(13, 27, 41, 55).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-03")
        given {
          requirementIsHours(8)
          todayIs(FRIDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(FRIDAY)
            from("20:30")
            until("22:30")
            startingInDays(-14)
          }
        }

        then {
          shouldCreateAppointments {
            listOf(0, 14, 28, 42).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-04")
        given {
          requirementIsHours(8)
          todayIs(SATURDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(FRIDAY)
            from("00:00")
            until("02:00")
            startingInDays(-8)
          }
        }

        then {
          shouldCreateAppointments {
            listOf(6, 20, 34, 48).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-05")
        given {
          requirementIsHours(8)
          todayIs(FRIDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(THURSDAY)
            from("11:00")
            until("13:00")
            startingInDays(-1)
          }
        }

        then {
          shouldCreateAppointments {
            listOf(13, 27, 41, 55).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-06")
        given {
          requirementIsHours(8)
          todayIs(FRIDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(FRIDAY)
            from("12:00")
            until("14:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            listOf(0, 14, 28, 42).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-07")
        given {
          requirementIsHours(8)
          todayIs(SATURDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(DayOfWeek.SUNDAY)
            from("13:00")
            until("15:00")
            startingInDays(1)
          }
        }

        then {
          shouldCreateAppointments {
            listOf(1, 15, 29, 43).forEach { offset ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(offset)
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
        scenarioId("FREQ-FN-08")
        given {
          requirementIsHours(52)
          todayIs(FRIDAY)
          projectExistsWithCode("PROJ1")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(FORTNIGHTLY)
            onWeekDay(FRIDAY)
            from("14:00")
            until("15:00")
            startingToday()
          }
        }

        then {
          shouldCreateAppointments {
            (0..51).forEach { weeks ->
              appointment {
                projectCode("PROJ1")
                allocation("ALLOC1")
                todayWithOffsetDays(weeks * 14)
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
        scenarioId("FREQ-MIXED-01")
        given {
          requirementIsHours(37)
          todayIs(FRIDAY)
          projectExistsWithCode("PROJ1")
          projectExistsWithCode("PROJ2")
          projectExistsWithCode("PROJ3")
          projectExistsWithCode("PROJ4")
          projectExistsWithCode("PROJ5")
          projectExistsWithCode("PROJ6")

          allocation {
            alias("ALLOC1")
            projectCode("PROJ1")
            frequency(WEEKLY)
            onWeekDay(FRIDAY)
            from("10:00")
            until("12:00")
            startingToday()
          }

          allocation {
            alias("ALLOC2")
            projectCode("PROJ2")
            frequency(FORTNIGHTLY)
            onWeekDay(SATURDAY)
            from("10:00")
            until("13:00")
            startingToday()
          }

          allocation {
            alias("ALLOC3")
            projectCode("PROJ3")
            frequency(WEEKLY)
            onWeekDay(DayOfWeek.SUNDAY)
            from("10:00")
            until("14:00")
            startingToday()
          }

          allocation {
            alias("ALLOC4")
            projectCode("PROJ4")
            frequency(ONCE)
            onWeekDay(MONDAY)
            from("10:00")
            until("15:00")
            startingInDays(3)
          }

          allocation {
            alias("ALLOC5")
            projectCode("PROJ5")
            frequency(FORTNIGHTLY)
            onWeekDay(THURSDAY)
            from("10:00")
            until("16:00")
            startingInDays(13)
          }

          allocation {
            alias("ALLOC6")
            projectCode("PROJ6")
            frequency(ONCE)
            onWeekDay(DayOfWeek.WEDNESDAY)
            from("10:00")
            until("17:00")
            startingInDays(22)
          }
        }

        then {
          shouldCreateAppointments {
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays()
              from("10:00")
              until("12:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(1)
              from("10:00")
              until("13:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(2)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ4")
              allocation("ALLOC4")
              todayWithOffsetDays(3)
              from("10:00")
              until("15:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(7)
              from("10:00")
              until("12:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(9)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ5")
              allocation("ALLOC5")
              todayWithOffsetDays(13)
              from("10:00")
              until("16:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(14)
              from("10:00")
              until("12:00")
            }
            appointment {
              projectCode("PROJ2")
              allocation("ALLOC2")
              todayWithOffsetDays(15)
              from("10:00")
              until("13:00")
            }
            appointment {
              projectCode("PROJ3")
              allocation("ALLOC3")
              todayWithOffsetDays(16)
              from("10:00")
              until("14:00")
            }
            appointment {
              projectCode("PROJ1")
              allocation("ALLOC1")
              todayWithOffsetDays(21)
              from("10:00")
              until("12:00")
            }
          }
        }
      }
    }

    @NDeliusDataModelsRequired
    fun `FREQ-MIXED-02 Use largest frequency defined between Allocation and linked Availability`() {
      // see documentation on @NDeliusDataModelsRequired
    }
  }
}

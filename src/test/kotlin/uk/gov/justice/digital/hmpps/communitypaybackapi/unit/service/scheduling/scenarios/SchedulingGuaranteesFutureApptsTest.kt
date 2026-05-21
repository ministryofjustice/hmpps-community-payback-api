package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingFrequency.WEEKLY
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.SchedulingTriggerType
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY

class SchedulingGuaranteesFutureApptsTest {
  @ParameterizedTest
  @ArgumentsSource(NonAllocationChangeTriggerTypes::class)
  fun `FUTURE-APPTS-01 If scheduling an appointment today would create it in the past, then it should be skipped`(triggerType: SchedulingTriggerType) {
    schedulingScenario {
      scenarioId("FUTURE-APPTS-01")
      given {
        requirementHoursAre(8)
        todayIs(MONDAY)
        timeIs(10, 30)
        projectExistsWithCode("PROJ1")
        projectExistsWithCode("PROJ2")
        schedulingTriggerTypeIs(triggerType)

        allocation {
          alias("ALLOC1")
          projectCode("PROJ1")
          frequency(WEEKLY)
          onWeekDay(MONDAY)
          from("10:00")
          until("18:00")
        }

        allocation {
          alias("ALLOC2")
          projectCode("PROJ2")
          frequency(WEEKLY)
          onWeekDay(TUESDAY)
          from("16:00")
          until("20:00")
        }
      }

      then {
        shouldCreateAppointments {
          appointment {
            projectCode("PROJ2")
            allocation("ALLOC2")
            todayWithOffsetDays(1)
            from("16:00")
            until("20:00")
          }

          appointment {
            projectCode("PROJ1")
            allocation("ALLOC1")
            todayWithOffsetDays(7)
            from("10:00")
            until("14:00")
          }
        }
      }
    }
  }
}

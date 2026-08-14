package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.validation

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.LocalDate

data class TestData(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val notes: String?,
  val numberOfAttendees: Int,
  val isWeeklyEvent: Boolean,
) {
  companion object {
    fun valid(): TestData {
      val startDate = randomLocalDate()
      val endDate = startDate.plusDays(Long.random(1, 7))
      val notes = String.random(length = 400)
      val numberOfAttendees = Int.random(5, 25)
      val isWeeklyEvent = Boolean.random()

      return TestData(
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        numberOfAttendees = numberOfAttendees,
        isWeeklyEvent = isWeeklyEvent,
      )
    }
  }
}

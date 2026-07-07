package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate
import java.time.LocalTime

data class ProjectDto(
  val projectName: String,
  val projectCode: String,
  val projectType: ProjectTypeDto,
  val providerName: String,
  val providerCode: String,
  val teamName: String,
  val teamCode: String,
  val location: LocationDto,
  val hiVisRequired: Boolean,
  val beneficiaryDetails: BeneficiaryDetailsDto,
  val expectedEndDateExclusive: LocalDate?,
  val actualEndDateExclusive: LocalDate?,
  val availability: List<ProjectAvailabilityDto>,
) {
  companion object
}

data class ProjectAvailabilityDto(
  val frequency: SchedulingFrequencyDto?,
  val dayOfWeek: SchedulingDayOfWeekDto,
  val startDateInclusive: LocalDate?,
  val endDateExclusive: LocalDate?,
  val startTime: LocalTime?,
  val endTime: LocalTime?,
) {
  companion object
}

enum class SchedulingFrequencyDto {
  ONCE,
  WEEKLY,
  FORTNIGHTLY,
}

enum class SchedulingDayOfWeekDto {
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY,
  SUNDAY,
}

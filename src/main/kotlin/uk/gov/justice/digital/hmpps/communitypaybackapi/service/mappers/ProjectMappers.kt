package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAvailability
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeStats
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BeneficiaryDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectAvailabilityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectOutcomeSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingDayOfWeekDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SchedulingFrequencyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import java.time.DayOfWeek

fun NDProject.toDto(
  projectType: ProjectTypeEntity,
) = ProjectDto(
  projectName = this.name,
  projectCode = this.code,
  projectType = projectType.toDto(),
  providerCode = this.provider.code,
  teamCode = this.team.code,
  location = this.location.toDto(),
  hiVisRequired = this.hiVisRequired,
  beneficiaryDetails = this.beneficiary.toDto(),
  expectedEndDateExclusive = this.expectedEndDateExclusive,
  actualEndDateExclusive = this.actualEndDateExclusive,
  availability = this.availability.map { it.toDto() },
)

fun NDBeneficiaryDetails.toDto() = BeneficiaryDetailsDto(
  beneficiary = this.name,
  contactName = this.contactName,
  emailAddress = this.emailAddress,
  website = this.website,
  telephoneNumber = this.telephoneNumber,
  location = location?.toDto(),
)

fun NDProjectOutcomeStats.toDto() = ProjectOutcomeSummaryDto(
  projectName = this.project.name,
  projectCode = this.project.code,
  location = this.project.location.toDto(),
  numberOfAppointmentsOverdue = this.overdueOutcomesCount,
  oldestOverdueAppointmentInDays = this.oldestOverdueInDays,
)

fun NDProjectAvailability.toDto() = ProjectAvailabilityDto(
  frequency = this.frequency?.toDto(),
  dayOfWeek = dayOfWeek.toDto(),
  startDateInclusive = startDateInclusive,
  endDateExclusive = endDateExclusive,
)

fun NDSchedulingFrequency.toDto() = when (this) {
  NDSchedulingFrequency.Once -> SchedulingFrequencyDto.ONCE
  NDSchedulingFrequency.Weekly -> SchedulingFrequencyDto.WEEKLY
  NDSchedulingFrequency.Fortnightly -> SchedulingFrequencyDto.FORTNIGHTLY
}

fun NDSchedulingDayOfWeek.toDto() = when (this) {
  NDSchedulingDayOfWeek.Monday -> SchedulingDayOfWeekDto.MONDAY
  NDSchedulingDayOfWeek.Tuesday -> SchedulingDayOfWeekDto.TUESDAY
  NDSchedulingDayOfWeek.Wednesday -> SchedulingDayOfWeekDto.WEDNESDAY
  NDSchedulingDayOfWeek.Thursday -> SchedulingDayOfWeekDto.THURSDAY
  NDSchedulingDayOfWeek.Friday -> SchedulingDayOfWeekDto.FRIDAY
  NDSchedulingDayOfWeek.Saturday -> SchedulingDayOfWeekDto.SATURDAY
  NDSchedulingDayOfWeek.Sunday -> SchedulingDayOfWeekDto.SUNDAY
}

fun SchedulingDayOfWeekDto.toDayOfWeek() = when (this) {
  SchedulingDayOfWeekDto.MONDAY -> DayOfWeek.MONDAY
  SchedulingDayOfWeekDto.TUESDAY -> DayOfWeek.TUESDAY
  SchedulingDayOfWeekDto.WEDNESDAY -> DayOfWeek.WEDNESDAY
  SchedulingDayOfWeekDto.THURSDAY -> DayOfWeek.THURSDAY
  SchedulingDayOfWeekDto.FRIDAY -> DayOfWeek.FRIDAY
  SchedulingDayOfWeekDto.SATURDAY -> DayOfWeek.SATURDAY
  SchedulingDayOfWeekDto.SUNDAY -> DayOfWeek.SUNDAY
}

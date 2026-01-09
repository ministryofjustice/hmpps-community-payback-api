package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RequirementProgress
import java.time.DayOfWeek
import java.time.Duration

fun RequirementProgress.toSchedulingRequirementProgress() = SchedulingRequirementProgress(
  lengthMinutes = Duration.ofMinutes((this.requiredMinutes - this.completedMinutes + this.adjustments).toLong()),
)

fun List<NDSchedulingAllocation>.toSchedulingAllocations() = SchedulingAllocations(
  allocations = this
    /*
    This matches existing logic in the NDelius scheduling code. Validation in the NDelius UI
    _should_ ensure the following filters always return true for all appointments
     */
    .filter { it.endDateInclusive == null || it.endDateInclusive.isAfter(it.startDateInclusive) }
    .filter { it.startTime.isBefore(it.endTime) }
    .map { it.toSchedulingAllocation() },
)

fun NDSchedulingAllocation.toSchedulingAllocation() = SchedulingAllocation(
  id = this.id,
  alias = "ALLOC${this.id}",
  project = this.project.toSchedulingProject(),
  frequency = determineFrequency(
    allocationFrequency = this.frequency,
    availabilityFrequency = this.projectAvailability?.frequency,
  ),
  dayOfWeek = when (this.dayOfWeek) {
    NDSchedulingDayOfWeek.MONDAY -> DayOfWeek.MONDAY
    NDSchedulingDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
    NDSchedulingDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
    NDSchedulingDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
    NDSchedulingDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
    NDSchedulingDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
    NDSchedulingDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
  },
  startDateInclusive = this.startDateInclusive,
  endDateInclusive = this.determineEndDateInclusive(),
  startTime = this.startTime,
  endTime = this.endTime,
)

fun List<NDSchedulingAppointment>.toSchedulingExistingAppointments() = SchedulingExistingAppointments(
  appointments = this.map { it.toSchedulingExistingAppointment() },
)

fun NDSchedulingAppointment.toSchedulingExistingAppointment() = SchedulingExistingAppointment(
  project = project.toSchedulingProject(),
  date = date,
  startTime = startTime,
  endTime = endTime,
  hasOutcome = outcome != null,
  timeCredited = timeCredited,
  allocation = allocation?.toSchedulingAllocation(),
)

fun SchedulingRequiredAppointment.toNDCreateAppointment() = NDCreateAppointment(
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  providerCode = Code(this.project.providerCode),
  teamCode = Code(this.project.teamCode),
  projectCode = Code(this.project.code),
  projectTypeCode = Code(this.project.projectTypeCode),
  allocationId = this.allocation?.id,
)

private fun NDSchedulingAllocation.determineEndDateInclusive() = this.endDateInclusive ?: listOfNotNull(
  project.expectedEndDateExclusive,
  project.actualEndDateExclusive,
  projectAvailability?.endDateExclusive,
).minOrNull()?.minusDays(1)

private fun determineFrequency(
  allocationFrequency: NDSchedulingFrequency?,
  availabilityFrequency: NDSchedulingFrequency?,
): SchedulingFrequency {
  val frequencies = listOfNotNull(allocationFrequency, availabilityFrequency)

  return when {
    frequencies.isEmpty() -> SchedulingFrequency.WEEKLY
    NDSchedulingFrequency.FORTNIGHTLY in frequencies -> SchedulingFrequency.FORTNIGHTLY
    NDSchedulingFrequency.WEEKLY in frequencies -> SchedulingFrequency.WEEKLY
    else -> SchedulingFrequency.ONCE
  }
}

private fun NDSchedulingProject.toSchedulingProject() = SchedulingProject(
  code = code.code,
  projectTypeCode = projectTypeCode.code,
  providerCode = providerCode.code,
  teamCode = teamCode.code,
)

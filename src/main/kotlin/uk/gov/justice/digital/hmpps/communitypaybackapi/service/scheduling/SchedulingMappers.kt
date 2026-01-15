package uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingDayOfWeek
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import java.time.DayOfWeek
import java.time.Duration

fun NDRequirementProgress.toSchedulingRequirement(
  crn: String,
  eventNumber: Int,
) = SchedulingRequirement(
  crn = crn,
  deliusEventNumber = eventNumber,
  requirementLengthMinutes = Duration.ofMinutes((this.requiredMinutes + this.adjustments).toLong()),
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
  alias = "ALLOC ${this.id}",
  project = this.project.toSchedulingProject(),
  frequency = determineFrequency(
    allocationFrequency = this.frequency,
    availabilityFrequency = this.projectAvailability?.frequency,
  ),
  dayOfWeek = when (this.dayOfWeek) {
    NDSchedulingDayOfWeek.Monday -> DayOfWeek.MONDAY
    NDSchedulingDayOfWeek.Tuesday -> DayOfWeek.TUESDAY
    NDSchedulingDayOfWeek.Wednesday -> DayOfWeek.WEDNESDAY
    NDSchedulingDayOfWeek.Thursday -> DayOfWeek.THURSDAY
    NDSchedulingDayOfWeek.Friday -> DayOfWeek.FRIDAY
    NDSchedulingDayOfWeek.Saturday -> DayOfWeek.SATURDAY
    NDSchedulingDayOfWeek.Sunday -> DayOfWeek.SUNDAY
  },
  startDateInclusive = this.startDateInclusive,
  endDateInclusive = this.determineEndDateInclusive(),
  startTime = this.startTime,
  endTime = this.endTime,
)

fun List<NDSchedulingExistingAppointment>.toSchedulingExistingAppointments() = SchedulingExistingAppointments(
  appointments = this.map { it.toSchedulingExistingAppointment() },
)

fun NDSchedulingExistingAppointment.toSchedulingExistingAppointment() = SchedulingExistingAppointment(
  id = id,
  projectCode = project.code,
  date = date,
  startTime = startTime,
  endTime = endTime,
  hasOutcome = outcome != null,
  minutesCredited = minutesCredited?.let { Duration.ofMinutes(it) },
  allocationId = allocationId,
)

fun SchedulingRequiredAppointment.toNDCreateAppointment() = NDCreateAppointment(
  date = this.date,
  startTime = this.startTime,
  endTime = this.endTime,
  providerCode = Code(this.project.providerCode),
  teamCode = Code(this.project.teamCode),
  projectCode = Code(this.project.code),
  projectTypeCode = Code(this.project.projectTypeCode),
  allocationId = this.allocation.id,
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
    NDSchedulingFrequency.Fortnightly in frequencies -> SchedulingFrequency.FORTNIGHTLY
    NDSchedulingFrequency.Weekly in frequencies -> SchedulingFrequency.WEEKLY
    else -> SchedulingFrequency.ONCE
  }
}

private fun NDSchedulingProject.toSchedulingProject() = SchedulingProject(
  code = code,
  projectTypeCode = type.code,
  providerCode = provider.code,
  teamCode = team.code,
)

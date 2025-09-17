package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.AppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationsDto

fun ProjectAllocations.toDto() = ProjectAllocationsDto(this.allocations.map { it.toDto() })
fun ProjectAllocation.toDto() = ProjectAllocationDto(
  this.id,
  this.projectId,
  this.projectName,
  this.projectCode,
  this.date,
  this.startTime,
  this.endTime,
  this.numberOfOffendersAllocated,
  this.numberOfOffendersWithOutcomes,
  this.numberOfOffendersWithEA,
)

fun ProjectAppointments.toDto(
  offenderInfoResults: List<OffenderInfoResult>,
) = AppointmentsDto(this.appointments.map { it.toDto(offenderInfoResults) })

fun ProjectAppointment.toDto(
  offenderInfoResults: List<OffenderInfoResult>,
) = AppointmentDto(
  id = this.id,
  projectName = this.projectName,
  requirementMinutes = this.requirementMinutes,
  completedMinutes = this.completedMinutes,
  offender = offenderInfoResults.first { it.crn == this.crn }.toDto(),
)

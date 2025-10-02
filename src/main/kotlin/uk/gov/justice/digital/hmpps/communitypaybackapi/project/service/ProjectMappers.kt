package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.ProjectAllocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.ProjectAllocationsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.SessionDto

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
) = SessionDto(
  this.appointments.map { appointment ->
    appointment.toDto(offenderInfoResults.first { offender -> appointment.crn == offender.crn })
  },
)

fun ProjectAppointment.toDto(
  offenderInfoResult: OffenderInfoResult,
) = AppointmentDto(
  id = this.id,
  projectName = this.projectName,
  projectCode = this.projectCode,
  requirementMinutes = this.requirementMinutes,
  completedMinutes = this.completedMinutes,
  offender = offenderInfoResult.toDto(),
)

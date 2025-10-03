package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentSummaryDto
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

fun ProjectSession.toDto(offenderInfoResults: List<OffenderInfoResult>) = SessionDto(
  projectCode = this.projectCode,
  projectName = this.projectName,
  projectLocation = this.projectLocation,
  startTime = this.startTime,
  endTime = this.endTime,
  date = this.date,
  appointmentSummaries = this.appointmentSummaries.toDtos(offenderInfoResults),
)

fun List<ProjectAppointmentSummary>.toDtos(offenderInfoResults: List<OffenderInfoResult>) = this.map { it.toDto(offenderInfoResults) }

fun ProjectAppointmentSummary.toDto(
  offenderInfoResults: List<OffenderInfoResult>,
) = AppointmentSummaryDto(
  id = this.id,
  requirementMinutes = this.requirementMinutes,
  completedMinutes = this.completedMinutes,
  offender = offenderInfoResults.first { it.crn == this.crn }.toDto(),
)

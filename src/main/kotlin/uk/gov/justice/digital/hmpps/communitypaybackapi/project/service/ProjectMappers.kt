package uk.gov.justice.digital.hmpps.communitypaybackapi.project.service

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.controller.ProjectAllocationsDto

fun ProjectAllocations.toDto() = ProjectAllocationsDto(this.allocations.map { it.toDto() })
fun ProjectAllocation.toDto() = ProjectAllocationDto(
  this.id,
  this.projectName,
  this.projectCode,
  this.date,
  this.startTime,
  this.endTime,
  this.numberOfOffendersAllocated,
  this.numberOfOffendersWithOutcomes,
  this.numberOfOffendersWithEA,
)

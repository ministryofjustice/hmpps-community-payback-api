package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

import java.time.LocalDate

data class ProjectDto(
  val projectName: String,
  val projectCode: String,
  val projectType: ProjectTypeDto,
  val providerCode: String,
  val teamCode: String,
  val location: LocationDto,
  val hiVisRequired: Boolean,
  val beneficiaryDetails: BeneficiaryDetailsDto,
  val expectedEndDateExclusive: LocalDate?,
  val actualEndDateExclusive: LocalDate?,
) {
  companion object
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.dto

data class ProjectDto(
  val projectName: String,
  val projectCode: String,
  val projectType: ProjectTypeDto,
  val location: LocationDto,
  val hiVisRequired: Boolean,
  val beneficiaryDetails: BeneficiaryDetailsDto,
) {
  companion object
}

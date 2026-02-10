package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProjectAndLocation.Companion.valid() = NDProjectAndLocation(
  code = String.random(),
  name = String.random(),
  location = NDAddress.valid(),
)

fun NDProject.Companion.valid() = NDProject(
  code = String.random(),
  name = String.random(),
  projectTypeCode = String.random(),
  location = NDAddress.valid(),
  beneficiaryDetails = NDBeneficiaryDetails.valid(),
  hiVisRequired = Boolean.random(),
)

fun NDProjectOutcomeSummary.Companion.valid() = NDProjectOutcomeSummary(
  code = String.random(),
  name = String.random(),
  location = NDAddress.valid(),
  overdueOutcomesCount = Int.random(),
  oldestOverdueInDays = Int.random(),
)

fun NDProject.Companion.valid(ctx: ApplicationContext): NDProject {
  val projectType = ctx.getBean<ProjectTypeEntityRepository>().findAll().first()

  return NDProject.valid().copy(projectTypeCode = projectType.code)
}

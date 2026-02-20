package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun NDProjectAndLocation.Companion.valid() = NDProjectAndLocation(
  code = String.random(),
  name = String.random(),
  location = NDAddress.valid(),
)

fun NDProject.Companion.valid() = NDProject(
  code = String.random(),
  name = String.random(),
  type = NDProjectType.valid(),
  provider = NDCode.valid(),
  team = NDCode.valid(),
  location = NDAddress.valid(),
  beneficiary = NDBeneficiaryDetails.valid(),
  hiVisRequired = Boolean.random(),
  expectedEndDateExclusive = randomLocalDate(),
  actualEndDateExclusive = randomLocalDate(),
)

fun NDProjectOutcomeSummary.Companion.valid() = NDProjectOutcomeSummary(
  code = String.random(),
  name = String.random(),
  location = NDAddress.valid(),
  overdueOutcomesCount = Int.random(),
  oldestOverdueInDays = Int.random(),
)

fun NDProject.Companion.valid(ctx: ApplicationContext): NDProject = NDProject.valid().copy(type = NDProjectType.valid(ctx))

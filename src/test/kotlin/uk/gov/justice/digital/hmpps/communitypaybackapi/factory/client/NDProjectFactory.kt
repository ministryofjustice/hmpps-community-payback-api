package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDBeneficiaryDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import kotlin.String

fun NDBeneficiaryDetails.Companion.valid() = NDBeneficiaryDetails(
  name = String.random(50),
  contactName = String.random(50),
  emailAddress = String.random(50) + "@localhost",
  website = String.random(50),
  telephoneNumber = String.random(20),
  location = NDAddress.valid(),
)

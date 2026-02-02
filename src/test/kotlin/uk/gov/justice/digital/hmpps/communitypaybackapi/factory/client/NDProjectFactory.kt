package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAddress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProject.Companion.valid() = NDProject(
  code = String.random(),
  name = String.random(),
  location = NDAddress.valid(),
)

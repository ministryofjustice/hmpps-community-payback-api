package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun SupervisorName.Companion.valid() = SupervisorName(
  forename = String.random(50),
  surname = String.random(50),
  middleName = String.random(50),
)

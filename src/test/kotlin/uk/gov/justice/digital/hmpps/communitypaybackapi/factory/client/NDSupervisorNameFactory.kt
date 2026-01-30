package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDSupervisorName.Companion.valid() = NDSupervisorName(
  forename = String.random(50),
  surname = String.random(50),
  middleName = String.random(50),
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDName
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDName.Companion.valid() = NDName(
  forename = String.random(10),
  surname = String.random(10),
  middleNames = emptyList(),
)

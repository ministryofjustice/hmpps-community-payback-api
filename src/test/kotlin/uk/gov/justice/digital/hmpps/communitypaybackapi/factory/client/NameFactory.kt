package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Name
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun Name.Companion.valid() = Name(
  forename = String.random(10),
  surname = String.random(10),
  middleNames = emptyList(),
)

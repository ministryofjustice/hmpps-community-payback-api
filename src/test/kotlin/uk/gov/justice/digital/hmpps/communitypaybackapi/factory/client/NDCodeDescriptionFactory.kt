package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDCodeDescription.Companion.valid() = NDCodeDescription(
  description = String.random(20),
  code = String.random(5),
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ContactOutcome.Companion.valid() = ContactOutcome(
  code = String.random(5),
  description = String.random(20),
)

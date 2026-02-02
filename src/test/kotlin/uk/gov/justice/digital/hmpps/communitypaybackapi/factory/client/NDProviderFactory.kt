package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProvider
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProvider.Companion.valid() = NDProvider(
  code = String.random(),
  name = String.random(),
)

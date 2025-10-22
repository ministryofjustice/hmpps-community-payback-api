package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Provider
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun Provider.Companion.valid() = Provider(
  code = String.Companion.random(),
  name = String.random(),
)

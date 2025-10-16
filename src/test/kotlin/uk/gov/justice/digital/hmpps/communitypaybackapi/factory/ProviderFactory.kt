package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Provider

fun Provider.Companion.valid() = Provider(
  code = String.random(),
  name = String.random(),
)

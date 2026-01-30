package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProjectType.Companion.valid() = NDProjectType(
  code = String.random(),
  name = String.random(),
)

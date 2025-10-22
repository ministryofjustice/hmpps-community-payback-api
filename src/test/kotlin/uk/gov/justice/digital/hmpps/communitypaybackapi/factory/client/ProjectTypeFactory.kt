package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ProjectType.Companion.valid() = ProjectType(
  code = String.Companion.random(),
  name = String.random(),
)

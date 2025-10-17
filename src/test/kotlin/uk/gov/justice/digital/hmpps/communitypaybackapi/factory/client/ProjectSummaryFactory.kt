package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ProjectSummary.Companion.valid() = ProjectSummary(
  code = String.random(5),
  name = String.random(50),
)

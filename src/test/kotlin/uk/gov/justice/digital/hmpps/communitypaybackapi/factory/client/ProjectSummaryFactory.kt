package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ProjectSummary.Companion.valid() = ProjectSummary(
  code = String.random(5),
  description = String.random(50),
)

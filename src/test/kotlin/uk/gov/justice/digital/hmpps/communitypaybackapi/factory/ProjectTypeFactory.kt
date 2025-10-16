package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectType

fun ProjectType.Companion.valid() = ProjectType(
  code = String.random(),
  name = String.random(),
)

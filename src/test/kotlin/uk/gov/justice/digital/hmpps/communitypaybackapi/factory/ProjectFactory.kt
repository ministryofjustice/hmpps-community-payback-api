package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project

fun Project.Companion.valid() = Project(
  code = String.random(),
  name = String.random(),
)

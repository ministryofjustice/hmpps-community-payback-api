package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation

fun Project.Companion.valid() = Project(
  code = String.random(),
  name = String.random(),
  location = ProjectLocation.valid(),
)

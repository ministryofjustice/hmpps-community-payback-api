package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun Project.Companion.valid() = Project(
  code = String.Companion.random(),
  name = String.random(),
  location = ProjectLocation.valid(),
)

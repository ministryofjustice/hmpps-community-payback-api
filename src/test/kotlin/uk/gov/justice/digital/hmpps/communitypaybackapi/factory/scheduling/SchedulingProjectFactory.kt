package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import kotlin.String

fun SchedulingProject.Companion.valid() = SchedulingProject(
  code = String.random(5),
  projectTypeCode = String.random(5),
  providerCode = String.random(5),
  teamCode = String.random(5),
)

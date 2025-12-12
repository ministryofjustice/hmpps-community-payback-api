package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling

import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject

fun SchedulingProject.Companion.valid() = SchedulingProject(
  code = String.random(5),
)

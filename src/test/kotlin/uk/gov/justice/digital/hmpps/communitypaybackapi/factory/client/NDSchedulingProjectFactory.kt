package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.String

fun NDSchedulingProject.Companion.valid() = NDSchedulingProject(
  name = String.random(20),
  code = Code(String.random(5)),
  type = NDNameCode.valid(),
  provider = NDNameCode.valid(),
  team = NDNameCode.valid(),
  expectedEndDateExclusive = randomLocalDate(),
  actualEndDateExclusive = randomLocalDate(),
)

fun NDSchedulingProject.Companion.validNoEndDate() = valid().copy(
  expectedEndDateExclusive = null,
  actualEndDateExclusive = null,
)

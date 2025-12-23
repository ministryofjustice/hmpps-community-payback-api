package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Code
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import kotlin.String

fun NDSchedulingProject.Companion.valid() = NDSchedulingProject(
  code = Code(String.random(5)),
  projectTypeCode = Code(String.random(5)),
  providerCode = Code(String.random(5)),
  teamCode = Code(String.random(5)),
  expectedEndDateExclusive = randomLocalDate(),
  actualEndDateExclusive = randomLocalDate(),
)

fun NDSchedulingProject.Companion.validNoEndDate() = valid().copy(
  expectedEndDateExclusive = null,
  actualEndDateExclusive = null,
)

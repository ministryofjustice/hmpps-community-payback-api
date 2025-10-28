package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Grade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun SupervisorSummary.Companion.valid() = SupervisorSummary(
  name = SupervisorName.valid(),
  code = String.random(5),
  grade = Grade(
    code = String.random(5),
    description = String.random(50),
  ),
)

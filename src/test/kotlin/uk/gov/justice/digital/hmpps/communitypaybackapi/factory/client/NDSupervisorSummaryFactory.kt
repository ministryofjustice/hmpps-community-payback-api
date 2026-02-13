package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDGrade
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorName
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDSupervisorSummary.Companion.valid() = NDSupervisorSummary(
  name = NDSupervisorName.valid(),
  code = String.random(5),
  grade = NDGrade(
    code = String.random(5),
    description = String.random(50),
  ),
  unallocated = Boolean.random(),
)

fun NDSupervisorSummary.Companion.unallocated() = NDSupervisorSummary.valid().copy(
  unallocated = true,
  name = NDSupervisorName.valid().copy(
    forename = "Unallocated",
    surname = "",
    middleName = null,
  ),
)

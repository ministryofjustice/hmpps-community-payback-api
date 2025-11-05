package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity

fun SessionSupervisorEntity.Companion.valid() = SessionSupervisorEntity(
  projectCode = String.random(5),
  day = randomLocalDate(),
  supervisorCode = String.random(5),
  allocatedByUsername = String.random(10),
)

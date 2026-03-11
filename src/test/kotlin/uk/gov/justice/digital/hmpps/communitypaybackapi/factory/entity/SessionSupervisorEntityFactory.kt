package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.SessionSupervisorEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun SessionSupervisorEntity.Companion.valid() = SessionSupervisorEntity(
  projectCode = String.random(5),
  day = randomLocalDate(),
  supervisorCode = String.random(5),
  allocatedByUsername = String.random(10),
)

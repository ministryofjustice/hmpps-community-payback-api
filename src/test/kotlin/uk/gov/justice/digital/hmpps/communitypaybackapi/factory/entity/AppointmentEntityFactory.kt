package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun AppointmentEntity.Companion.valid() = AppointmentEntity(
  id = UUID.randomUUID(),
  deliusId = Long.random(),
  crn = String.random(5),
  deliusEventNumber = Int.random().toLong(),
  createdByCommunityPayback = Boolean.random(),
)

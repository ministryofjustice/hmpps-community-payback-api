package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity

fun AppointmentEntity.toSarEntry(events: List<AppointmentEventEntity>) = mapOf(
  "date" to date,
  "createdByCommunityPayback" to createdByCommunityPayback,
  // this isn't rendered in the report, but is useful for testing to uniquely identify each record in testss
  "deliusEventNumber" to deliusEventNumber,
  "events" to events.map { it.toSarEntry() },
)

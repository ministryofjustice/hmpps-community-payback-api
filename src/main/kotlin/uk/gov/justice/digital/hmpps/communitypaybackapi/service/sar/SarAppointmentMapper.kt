package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity

fun AppointmentEntity.toSarEntry(
  adjustmentEvents: List<AdjustmentEventEntity>,
  appointmentEvents: List<AppointmentEventEntity>,
) = mapOf(
  "date" to date,
  "createdByCommunityPayback" to createdByCommunityPayback,
  // this isn't rendered in the report, but is useful for testing to uniquely identify each record in tests
  "deliusEventNumber" to deliusEventNumber,
  "appointmentEvents" to appointmentEvents.map { it.toSarEntry() },
  "adjustmentEvents" to adjustmentEvents.map { it.toSarEntry() },
)

package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity

fun AdjustmentEventEntity.toSarEntry() = mapOf(
  "triggeredAt" to triggeredAt,
  "triggerType" to triggerType,
  // this isn't rendered in the report, but is useful for testing to uniquely identify each record
  "deliusEventNumber" to appointment.deliusEventNumber,
  "eventType" to eventType.name,
  "adjustmentType" to adjustmentType.name,
  "adjustmentMinutes" to adjustmentMinutes,
  "adjustmentDate" to adjustmentDate,
  "adjustmentReason" to adjustmentReason.name,
)

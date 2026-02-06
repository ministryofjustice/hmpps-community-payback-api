package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventTriggerType

/**
 * There are some pending mappings TBD:
 *
 * **supervisorOfficerCode**
 * Not all supervisors are users in NDelius, so we can't provide a username that the SAR consumer can subsequently resolve a username.
 * We need to determine what (if anything) should be included given that internal identifiers shouldn't be included
 */
fun AppointmentEventEntity.toSarEntry() = mapOf(
  "triggeredAt" to triggeredAt,
  "triggerType" to triggerType,
  "triggeredByUsername" to when (triggerType) {
    AppointmentEventTriggerType.USER -> triggeredBy
    else -> null
  },
  // this isn't rendered in the report, but is useful for testing to uniquely identify each record
  "deliusEventNumber" to deliusEventNumber,
  "eventType" to eventType.name,
  "projectName" to projectName,
  "date" to date,
  "startTime" to startTime,
  "endTime" to endTime,
  "pickupLocation" to pickupLocationDescription,
  "pickupTime" to pickupTime,
  "notes" to notes,
  "contactOutcome" to contactOutcome?.name,
  "minutesCredited" to minutesCredited,
  "penaltyMinutes" to penaltyMinutes,
  "hiVisWorn" to hiVisWorn,
  "workedIntensively" to workedIntensively,
  "workQuality" to workQuality?.name,
  "behaviour" to behaviour?.name,
  "alertActive" to alertActive,
  "sensitive" to sensitive,
)

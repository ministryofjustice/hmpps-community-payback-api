package uk.gov.justice.digital.hmpps.communitypaybackapi.service.sar

import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity

fun EteCourseCompletionEventEntity.toSarEntry(
  appointmentEvent: AppointmentEventEntity?,
) = mapOf(
  "receivedAt" to receivedAt,
  "firstName" to firstName,
  "lastName" to lastName,
  "dateOfBirth" to dateOfBirth,
  "region" to region,
  "pdu" to pdu.name,
  "office" to office,
  "email" to email,
  "courseName" to courseName,
  "courseType" to courseType,
  "provider" to provider,
  "completionDateTime" to completionDateTime.toLocalDateTime(),
  "status" to status.name,
  "totalTimeMinutes" to totalTimeMinutes,
  "expectedTimeMinutes" to expectedTimeMinutes,
  "attempts" to attempts,
  "resolution" to resolution!!.run {
    mapOf(
      "decision" to resolution.name,
      "recordedAt" to createdAt,
      "recordedByUsername" to createdByUsername,
      "appointmentCreated" to (deliusAppointmentCreated == true),
      "appointmentUpdated" to (deliusAppointmentCreated == false),
      "minutesCredited" to minutesCredited,
      "appointment" to appointmentEvent?.toSarEntry(),
    )
  },
)

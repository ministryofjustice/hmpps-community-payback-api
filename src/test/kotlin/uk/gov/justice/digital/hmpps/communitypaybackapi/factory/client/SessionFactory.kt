package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.LocalTime

fun Session.Companion.valid() = Session(
  project = Project.valid(),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(17, 0),
  date = randomLocalDate(),
  appointmentSummaries = listOf(
    AppointmentSummary.valid(),
    AppointmentSummary.valid(),
  ),
)

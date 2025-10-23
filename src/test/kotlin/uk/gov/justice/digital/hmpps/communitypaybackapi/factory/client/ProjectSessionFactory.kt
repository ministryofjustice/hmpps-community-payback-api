package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.time.LocalTime

fun ProjectSession.Companion.valid() = ProjectSession(
  project = Project.valid(),
  startTime = LocalTime.of(9, 0),
  endTime = LocalTime.of(17, 0),
  date = randomLocalDate(),
  appointmentSummaries = listOf(
    ProjectAppointmentSummary.valid(),
    ProjectAppointmentSummary.valid(),
  ),
)

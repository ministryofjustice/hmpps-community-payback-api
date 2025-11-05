package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun Session.Companion.valid() = Session(
  project = Project.valid(),
  date = randomLocalDate(),
  appointmentSummaries = listOf(
    ProjectAppointmentSummary.valid(),
    ProjectAppointmentSummary.valid(),
  ),
)

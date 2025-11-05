package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun Session.Companion.valid() = Session(
  project = Project.valid(),
  date = randomLocalDate(),
  appointmentSummaries = listOf(
    AppointmentSummary.valid(),
    AppointmentSummary.valid(),
  ),
)

fun Session.Companion.valid(ctx: ApplicationContext) = Session.valid().copy(
  appointmentSummaries = listOf(
    AppointmentSummary.valid(ctx),
    AppointmentSummary.valid(ctx),
  ),
)

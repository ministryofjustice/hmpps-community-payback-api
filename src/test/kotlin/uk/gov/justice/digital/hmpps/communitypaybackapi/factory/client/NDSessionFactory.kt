package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSession

fun NDSession.Companion.valid() = NDSession(
  project = NDProject.valid(),
  appointmentSummaries = listOf(
    NDAppointmentSummary.valid(),
    NDAppointmentSummary.valid(),
  ),
)

fun NDSession.Companion.valid(ctx: ApplicationContext) = NDSession.valid().copy(
  appointmentSummaries = listOf(
    NDAppointmentSummary.valid(ctx),
    NDAppointmentSummary.valid(ctx),
  ),
)

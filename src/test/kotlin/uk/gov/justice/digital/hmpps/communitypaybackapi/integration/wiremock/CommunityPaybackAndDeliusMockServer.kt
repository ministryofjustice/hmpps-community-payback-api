package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Supervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object CommunityPaybackAndDeliusMockServer {

  val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun providers(
    username: String,
    providers: ProviderSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers?username=$username").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writeValueAsString(providers)),
      ),
    )
  }

  fun providerTeams(
    providerCode: String,
    providerTeams: ProviderTeamSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers/$providerCode/teams").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writeValueAsString(providerTeams)),
      ),
    )
  }

  fun getSessions(
    providerCode: String,
    teamCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    projectSessions: SessionSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers/$providerCode/teams/$teamCode/sessions?startDate=${startDate.toIsoDateString()}&endDate=${endDate.toIsoDateString()}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectSessions)),
        ),
    )
  }

  fun getSupervisor(
    username: String,
    supervisor: Supervisor,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/supervisors?username=$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(supervisor)),
        ),
    )
  }

  fun getSupervisorNotFound(
    username: String,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/supervisors?username=$username")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun getAppointmentNotFound(
    projectCode: String,
    appointmentId: Long,
    username: String,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/$projectCode/appointments/$appointmentId?username=$username")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun getAppointment(
    appointment: Appointment,
    username: String,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/${appointment.project.code}/appointments/${appointment.id}?username=$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(appointment)),
        ),
    )
  }

  fun postAppointments(
    projectCode: String,
  ) {
    WireMock.stubFor(
      post("/community-payback-and-delius/projects/$projectCode/appointments")
        .willReturn(
          aResponse().withStatus(200),
        ),
    )
  }

  @SuppressWarnings("LongParameterList")
  fun postAppointmentVerify(
    projectCode: String,
    expectedAppointments: List<ExpectedAppointmentCreate>,
  ) {
    var assertion = postRequestedFor(urlEqualTo("/community-payback-and-delius/projects/$projectCode/appointments"))

    expectedAppointments.forEachIndexed { index, expectedAppointment ->
      assertion = assertion
        .withRequestBody(matchingJsonPath("$.appointments[$index].crn", equalTo(expectedAppointment.crn)))
        .withRequestBody(matchingJsonPath("$.appointments[$index].eventNumber", equalTo(expectedAppointment.eventNumber.toString())))
        .withRequestBody(matchingJsonPath("$.appointments[$index].date", equalTo(expectedAppointment.date.toIsoDateString())))
        .withRequestBody(matchingJsonPath("$.appointments[$index].startTime", equalTo(expectedAppointment.startTime.format(DateTimeFormatter.ISO_TIME))))
        .withRequestBody(matchingJsonPath("$.appointments[$index].endTime", equalTo(expectedAppointment.endTime.format(DateTimeFormatter.ISO_TIME))))
    }

    WireMock.verify(assertion)
  }

  data class ExpectedAppointmentCreate(
    val crn: String,
    val eventNumber: Int,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
  )

  fun postAppointmentsVerifyZeroCalls() {
    WireMock.verify(0, postRequestedFor(urlMatching("/community-payback-and-delius/.*/appointments")))
  }

  fun putAppointment(
    projectCode: String,
    appointmentId: Long,
  ) {
    WireMock.stubFor(
      put("/community-payback-and-delius/projects/$projectCode/appointments/$appointmentId/outcome")
        .willReturn(
          aResponse().withStatus(200),
        ),
    )
  }

  fun putAppointmentNotFound(
    projectCode: String,
    appointmentId: Long,
  ) {
    WireMock.stubFor(
      put("/community-payback-and-delius/projects/$projectCode/appointments/$appointmentId/outcome")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun putAppointmentVerify(
    projectCode: String,
    appointmentId: Long,
  ) {
    WireMock.verify(putRequestedFor(urlEqualTo("/community-payback-and-delius/projects/$projectCode/appointments/$appointmentId/outcome")))
  }

  fun getProjectSession(
    username: String,
    date: LocalDate,
    session: Session,
  ) {
    WireMock.stubFor(
      get(
        "/community-payback-and-delius/projects/${session.project.code}/appointments" +
          "?date=${date.toIsoDateString()}&username=$username",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(session)),
        ),
    )
  }

  fun teamSupervisors(supervisorSummaries: SupervisorSummaries) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers/123/teams/99/supervisors")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(supervisorSummaries)),
        ),
    )
  }

  fun getUnpaidWorkRequirement(
    crn: String,
    eventNumber: Int,
    requirement: NDUnpaidWorkRequirement,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/case/$crn/event/$eventNumber/appointments/schedule")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(requirement)),
        ),
    )
  }

  fun getNonWorkingDays(
    nonWorkingDates: List<LocalDate>,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/reference-data/non-working-days")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(nonWorkingDates)),
        ),
    )
  }

  private fun LocalDate.toIsoDateString() = this.format(DateTimeFormatter.ISO_DATE)
}

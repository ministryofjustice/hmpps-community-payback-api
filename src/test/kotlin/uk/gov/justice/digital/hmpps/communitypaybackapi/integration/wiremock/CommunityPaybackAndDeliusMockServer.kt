package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Appointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Session
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.JacksonCustomConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CommunityPaybackAndDeliusMockServer {

  val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(JacksonCustomConfig().customModule())
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

  fun appointmentNotFound(projectCode: String, appointmentId: Long) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/$projectCode/appointments/$appointmentId")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun getAppointment(
    appointment: Appointment,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/${appointment.project.code}/appointments/${appointment.id}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(appointment)),
        ),
    )
  }

  fun putAppointment(
    id: Long,
  ) {
    WireMock.stubFor(
      put("/community-payback-and-delius/appointments/$id")
        .willReturn(
          aResponse().withStatus(200),
        ),
    )
  }

  fun putAppointmentNotFound(
    id: Long,
  ) {
    WireMock.stubFor(
      put("/community-payback-and-delius/appointments/$id")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun putAppointmentVerify(id: Long) {
    WireMock.verify(putRequestedFor(urlEqualTo("/community-payback-and-delius/appointments/$id")))
  }

  fun getProjectSession(
    username: String,
    session: Session,
  ) {
    WireMock.stubFor(
      get(
        "/community-payback-and-delius/projects/${session.project.code}/appointments" +
          "?date=${session.date.toIsoDateString()}&username=$username",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(session)),
        ),
    )
  }

  fun usersAccess(
    username: String,
    crns: List<String>,
    response: UserAccess,
  ) {
    WireMock.stubFor(
      post("/community-payback-and-delius/users/access?username=$username")
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(crns)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
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

  private fun LocalDate.toIsoDateString() = this.format(DateTimeFormatter.ISO_DATE)
}

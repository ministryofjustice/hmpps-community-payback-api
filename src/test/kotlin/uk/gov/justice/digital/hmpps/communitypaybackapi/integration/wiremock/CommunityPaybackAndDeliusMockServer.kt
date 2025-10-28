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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object CommunityPaybackAndDeliusMockServer {

  val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun providers(
    providers: ProviderSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers").willReturn(
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
    projectSessions: ProjectSessionSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/sessions?startDate=2025-01-09&endDate=2025-07-09&teamCode=999")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectSessions)),
        ),
    )
  }

  fun projectAppointmentNotFound(appointmentId: Long) {
    WireMock.stubFor(
      get("/community-payback-and-delius/appointments/$appointmentId")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun getAppointment(
    projectAppointment: ProjectAppointment,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/appointments/${projectAppointment.id}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectAppointment)),
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

  fun LocalTime.toHourMinuteString(): String = URLEncoder.encode(this.format(DateTimeFormatter.ofPattern("HH:mm")), "UTF-8")

  fun getProjectSession(
    projectSession: ProjectSession,
  ) {
    WireMock.stubFor(
      get(
        "/community-payback-and-delius/projects/${projectSession.project.code}/sessions/appointments" +
          "?date=${projectSession.date.toIsoDateString()}" +
          "&startTime=${projectSession.startTime.toHourMinuteString()}" +
          "&endTime=${projectSession.endTime.toHourMinuteString()}",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectSession)),
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

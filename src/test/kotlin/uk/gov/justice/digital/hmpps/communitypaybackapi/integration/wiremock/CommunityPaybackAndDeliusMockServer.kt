package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectSession
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object CommunityPaybackAndDeliusMockServer {

  val objectMapper = jacksonObjectMapper()
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
    providerId: Long,
    providerTeams: ProviderTeamSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/provider-teams?providerId=$providerId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writeValueAsString(providerTeams)),
      ),
    )
  }

  fun projectAllocations(
    projectAllocations: ProjectAllocations,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/project-allocations?startDate=2025-01-09&endDate=2025-07-09&teamId=999")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectAllocations)),
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

  fun projectAppointment(
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

  fun LocalTime.toHourMinuteString(): String = URLEncoder.encode(this.format(DateTimeFormatter.ofPattern("HH:mm")), "UTF-8")

  fun projectSessions(
    projectSession: ProjectSession,
  ) {
    WireMock.stubFor(
      get(
        "/community-payback-and-delius/projects/${projectSession.projectCode}/sessions/${projectSession.date.toIsoDateString()}" +
          "?start=${projectSession.startTime.toHourMinuteString()}" +
          "&end=${projectSession.endTime.toHourMinuteString()}",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectSession)),
        ),
    )
  }

  fun probationCasesSummaries(
    crns: List<String>,
    response: CaseSummaries,
  ) {
    WireMock.stubFor(
      post("/community-payback-and-delius/probation-cases/summaries")
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(crns)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
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

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ContactOutcomes
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectTypes
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.SupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import java.time.LocalDate
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

  fun projectAppointments(
    projectId: Long,
    date: LocalDate,
    projectAppointments: ProjectAppointments,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/$projectId/appointments?date=${date.toIsoDateString()}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectAppointments)),
        ),
    )
  }

  fun projectTypes(
    projectTypes: ProjectTypes,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/references/project-types")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(projectTypes)),
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

  fun contactOutcomes(contactOutcomes: ContactOutcomes) {
    WireMock.stubFor(
      get("/community-payback-and-delius/references/contact-outcomes")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writer().writeValueAsString(contactOutcomes)),
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

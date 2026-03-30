package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.absent
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentPostResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeStats
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProviderTeamSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSessionSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisor
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.unallocated
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.Long

object CommunityPaybackAndDeliusMockServer {

  val jsonMapper: JsonMapper = JsonMapper()

  fun providers(
    username: String,
    providers: NDProviderSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers?username=$username").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(jsonMapper.writeValueAsString(providers)),
      ),
    )
  }

  fun providerTeams(
    providerCode: String,
    providerTeams: NDProviderTeamSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers/$providerCode/teams").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(jsonMapper.writeValueAsString(providerTeams)),
      ),
    )
  }

  fun getProjectNotFound(
    projectCode: String,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/$projectCode")
        .willReturn(
          aResponse().withStatus(404),
        ),
    )
  }

  fun getProject(
    project: NDProject,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/${project.code}").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(jsonMapper.writeValueAsString(project)),
      ),
    )
  }

  fun getSessions(
    providerCode: String,
    teamCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    projectSessions: NDSessionSummaries,
    typeCode: List<String> = emptyList(),
  ) {
    val url = buildString {
      append("/community-payback-and-delius/providers/$providerCode/teams/$teamCode/sessions?startDate=${startDate.toIsoDateString()}&endDate=${endDate.toIsoDateString()}")
      typeCode.forEach {
        append("&typeCode=$it")
      }
    }

    WireMock.stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(projectSessions)),
        ),
    )
  }

  fun getSupervisor(
    username: String,
    supervisor: NDSupervisor,
  ) {
    println("This is " + jsonMapper.writeValueAsString(supervisor))

    WireMock.stubFor(
      get("/community-payback-and-delius/supervisors?username=$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(supervisor)),
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
    appointment: NDAppointment,
    username: String,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/projects/${appointment.project.code}/appointments/${appointment.id}?username=$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(appointment)),
        ),
    )
  }

  fun postAppointments(
    projectCode: String,
    appointmentCount: Int,
  ) {
    val response = (0..<appointmentCount).map { i ->
      mapOf(
        "id" to Long.random(),
        "reference" to $$"{{jsonPath request.body '$.appointments[$$i].reference'}}",
      )
    }

    WireMock.stubFor(
      post("/community-payback-and-delius/projects/$projectCode/appointments")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(response))
            .withTransformers("response-template"),
        ),
    )
  }

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

  fun postAppointmentVerify(
    projectCode: String,
    totalExpectedCalls: Int,
  ) {
    WireMock.verify(
      exactly(totalExpectedCalls),
      postRequestedFor(urlEqualTo("/community-payback-and-delius/projects/$projectCode/appointments")),
    )
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

  fun getTeamSupervisors(forProject: NDProject, supervisorSummaries: NDSupervisorSummaries) = getTeamSupervisors(
    providerCode = forProject.provider.code,
    teamCode = forProject.team.code,
    supervisorSummaries = supervisorSummaries,
  )

  fun getTeamSupervisors(
    providerCode: String,
    teamCode: String,
    supervisorSummaries: NDSupervisorSummaries,
  ) {
    WireMock.stubFor(
      get("/community-payback-and-delius/providers/$providerCode/teams/$teamCode/supervisors")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writer().writeValueAsString(supervisorSummaries)),
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
            .withBody(jsonMapper.writer().writeValueAsString(requirement)),
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
            .withBody(jsonMapper.writer().writeValueAsString(nonWorkingDates)),
        ),
    )
  }

  fun getProjects(
    providerCode: String,
    teamCode: String,
    projectTypeCodes: List<String> = emptyList(),
    response: List<NDProjectOutcomeStats>,
    pageNumber: Int = 0,
    pageSize: Int = 50,
    sortString: String = "name,desc",
  ) {
    val url = buildString {
      append("/community-payback-and-delius/providers/$providerCode/teams/$teamCode/projects?")
      projectTypeCodes.forEach {
        append("typeCode=$it&")
      }
      append("page=$pageNumber&size=$pageSize&sort=${URLEncoder.encode(sortString, "UTF-8")}")
    }

    val pageResponse = PageResponse(response, PageResponse.PageMeta(pageSize, pageNumber, response.size.toLong(), 1))

    WireMock.stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(pageResponse)),
        ),
    )
  }

  fun getAppointments(
    crn: String? = null,
    username: String,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    projectCodes: List<String> = emptyList(),
    pageNumber: Int = 0,
    pageSize: Int = 50,
    sortString: String = "name,desc",
    appointmentIds: List<Long> = emptyList(),
    appointments: List<NDAppointmentSummary> = emptyList(),
  ) {
    val url = buildString {
      append("/community-payback-and-delius/appointments")

      append("?username=$username")
      crn?.let { append("&crn=$it") }
      fromDate?.let { append("&fromDate=$it") }
      toDate?.let { append("&toDate=$it") }
      projectCodes.forEach {
        append("&projectCodes=$it")
      }
      appointmentIds.forEach {
        append("&appointmentIds=$it")
      }
      append("&page=$pageNumber")
      append("&size=$pageSize")
      append("&sort=${URLEncoder.encode(sortString, "UTF-8")}")
    }

    val pageResponse = PageResponse(appointments, PageResponse.PageMeta(pageSize, pageNumber, appointments.size.toLong(), 1))

    WireMock.stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(pageResponse)),
        ),
    )
  }

  fun getUpwDetailsSummary(
    crn: String,
    case: NDCaseSummary,
    unpaidWorkDetails: List<NDCaseDetail>,
    username: String? = null,
  ) {
    val ndCaseDetailsSummary = NDCaseDetailsSummary(case, unpaidWorkDetails)

    var builder = get(urlPathEqualTo("/community-payback-and-delius/case/$crn/summary"))

    builder = if (username != null) {
      builder.withQueryParam("username", equalTo(username))
    } else {
      builder.withQueryParam("username", absent())
    }

    WireMock.stubFor(
      builder.willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(jsonMapper.writeValueAsString(ndCaseDetailsSummary)),
      ),
    )
  }

  fun getUpwDetailsSummaryNotFound(crn: String) {
    WireMock.stubFor(
      get("/community-payback-and-delius/case/$crn/summary")
        .willReturn(
          aResponse()
            .withStatus(404),
        ),
    )
  }

  fun postAdjustment(username: String) {
    WireMock.stubFor(
      post("/community-payback-and-delius/adjustments?username=$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonMapper.writeValueAsString(listOf(NDAdjustmentPostResponse(1L))))
            .withTransformers("response-template"),
        ),
    )
  }

  fun postAdjustmentVerify(username: String) {
    WireMock.verify(postRequestedFor(urlEqualTo("/community-payback-and-delius/adjustments?username=$username")))
  }

  fun setupGetDataMocksForUpdateAppointment(
    existingAppointment: NDAppointment,
    project: NDProject,
    username: String,
  ) {
    getAppointment(
      appointment = existingAppointment,
      username = username,
    )
    setupGetDataMocksForCreateAppointment(
      crn = existingAppointment.case.crn,
      eventNumber = existingAppointment.event.number,
      project = project,
    )
  }

  fun setupGetDataMocksForCreateAppointment(
    crn: String,
    eventNumber: Int,
    project: NDProject,
  ) {
    getProject(project)
    getTeamSupervisors(
      forProject = project,
      supervisorSummaries = NDSupervisorSummaries(listOf(NDSupervisorSummary.unallocated())),
    )
    getUpwDetailsSummary(
      crn = crn,
      case = NDCaseSummary.valid(),
      unpaidWorkDetails = listOf(
        NDCaseDetail.valid().copy(
          eventNumber = eventNumber,
          sentenceDate = LocalDate.now().minusYears(10),
        ),
      ),
    )
  }

  private fun LocalDate.toIsoDateString() = this.format(DateTimeFormatter.ISO_DATE)
}

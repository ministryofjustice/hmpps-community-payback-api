package uk.gov.justice.digital.hmpps.communitypayback.stories.ui

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi

class E2E(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = CoreDsl.scenario("End to End Tests - Replay e2e-request-data.log")
    // Initial navigation and lookups
    .exec(
      HttpDsl.http("GET /admin/providers/N56/teams")
        .get("/admin/providers/N56/teams")
        .check(HttpDsl.status().`is`(200))
    )
    .exec(
      HttpDsl.http("GET /admin/projects/session-search")
        .get("/admin/projects/session-search?startDate=2025-08-07&endDate=2025-10-09&teamCode=N56DTX")
        .check(HttpDsl.status().`is`(200))
    )
    .exec(
      HttpDsl.http("GET /admin/projects/{projectCode}/sessions")
        .get("/admin/projects/N56123456/sessions/2025-09-22?startTime=09:00:00&endTime=17:00:00")
        .check(HttpDsl.status().`is`(200))
    )
    .exec(
      HttpDsl.http("GET /admin/appointments/{id}")
        .get("/admin/appointments/2500629219")
        .check(HttpDsl.status().`is`(200))
    )
    .exec(
      HttpDsl.http("GET /admin/providers/{code}/teams/{team}/supervisors")
        .get("/admin/providers/N56/teams/N56DTX/supervisors")
        .check(HttpDsl.status().`is`(200))
    )
    // Cycle 1 - ATTC attended complied
    .exec(
      HttpDsl.http("PUT form 230194a8 - supervisor only")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/230194a8-0532-460b-b4e5-e48cdddad4a3")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218"}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("GET /common/references/contact-outcomes")
        .get("/common/references/contact-outcomes")
        .check(HttpDsl.status().`is`(200))
    )
    .exec(
      HttpDsl.http("PUT form 230194a8 - with outcome")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/230194a8-0532-460b-b4e5-e48cdddad4a3")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218","contactOutcome":{"id":"37844d3a-6611-4c65-ad48-aeb793746589","name":"Attended - Complied","code":"ATTC","enforceable":false,"attended":true,"availableToSupervisors":false}}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("PUT form 230194a8 - with attendance and notes")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/230194a8-0532-460b-b4e5-e48cdddad4a3")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218","contactOutcome":{"id":"37844d3a-6611-4c65-ad48-aeb793746589","name":"Attended - Complied","code":"ATTC","enforceable":false,"attended":true,"availableToSupervisors":false},"startTime":"09:00","endTime":"17:00","attendanceData":{"penaltyTime":"01:00","hiVisWorn":true,"workedIntensively":true,"workQuality":"GOOD","behaviour":"POOR"},"notes":"They did a good job"}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("POST appointment outcome - ATTC")
        .post("/admin/appointments/2500629219/outcome")
        .asJson()
        .body(CoreDsl.StringBody("""{"deliusId":2500629219,"deliusVersionToUpdate":"6ab6dade-e96b-4243-95f7-d1b9ff023db4","alertActive":null,"sensitive":null,"startTime":"09:00","endTime":"17:00","contactOutcomeId":"37844d3a-6611-4c65-ad48-aeb793746589","attendanceData":{"penaltyTime":"01:00","hiVisWorn":true,"workedIntensively":true,"workQuality":"GOOD","behaviour":"POOR"},"supervisorOfficerCode":"N56A218","notes":"They did a good job","formKeyToDelete":{"id":"230194a8-0532-460b-b4e5-e48cdddad4a3","type":"APPOINTMENT_UPDATE_ADMIN"}}"""))
        .check(HttpDsl.status().`in`(200, 201))
    )
    // Cycle 2 - UAAB with enforcement ROM
    .exec(
      HttpDsl.http("PUT form fe619b49 - supervisor only")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/fe619b49-8b19-4dc1-bf8d-6c7767842058")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218"}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("PUT form fe619b49 - with UAAB outcome")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/fe619b49-8b19-4dc1-bf8d-6c7767842058")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218","contactOutcome":{"id":"9defbf76-cce5-438c-b01b-44d0facb6feb","name":"Unacceptable Absence","code":"UAAB","enforceable":true,"attended":false,"availableToSupervisors":false},"startTime":"09:00","endTime":"17:00","attendanceData":{"penaltyTime":"01:00"},"enforcement":{"action":{"id":"7ab7f428-f72a-4057-84b1-3342e04264a9","name":"Refer to Offender Manager","code":"ROM","respondByDateRequired":true},"respondBy":"2025-11-12"}}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("POST appointment outcome - UAAB with enforcement")
        .post("/admin/appointments/2500629219/outcome")
        .asJson()
        .body(CoreDsl.StringBody("""{"deliusId":2500629219,"deliusVersionToUpdate":"6ab6dade-e96b-4243-95f7-d1b9ff023db4","alertActive":null,"sensitive":null,"startTime":"09:00","endTime":"17:00","contactOutcomeId":"9defbf76-cce5-438c-b01b-44d0facb6feb","enforcementData":{"enforcementActionId":"7ab7f428-f72a-4057-84b1-3342e04264a9","respondBy":"2025-11-12"},"supervisorOfficerCode":"N56A218","formKeyToDelete":{"id":"fe619b49-8b19-4dc1-bf8d-6c7767842058","type":"APPOINTMENT_UPDATE_ADMIN"}}"""))
        .check(HttpDsl.status().`in`(200, 201))
    )
    // Cycle 3 - AFTC attended failed to comply with enforcement, notes
    .exec(
      HttpDsl.http("PUT form 9c4a7746 - supervisor only")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/9c4a7746-ac7b-4231-8479-f543220e8450")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218"}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("PUT form 9c4a7746 - with AFTC, attendance, notes, enforcement")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/9c4a7746-ac7b-4231-8479-f543220e8450")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218","contactOutcome":{"id":"599711cb-2abb-4df9-a8fb-83e5c3b86eea","name":"Attended - Failed to Comply","code":"AFTC","enforceable":true,"attended":true,"availableToSupervisors":false},"startTime":"09:00","endTime":"17:00","attendanceData":{"penaltyTime":"01:00","hiVisWorn":true,"workedIntensively":true,"workQuality":"GOOD","behaviour":"POOR"},"notes":"They did a good job","enforcement":{"action":{"id":"7ab7f428-f72a-4057-84b1-3342e04264a9","name":"Refer to Offender Manager","code":"ROM","respondByDateRequired":true},"respondBy":"2025-11-12"}}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("POST appointment outcome - AFTC with enforcement")
        .post("/admin/appointments/2500629219/outcome")
        .asJson()
        .body(CoreDsl.StringBody("""{"deliusId":2500629219,"deliusVersionToUpdate":"6ab6dade-e96b-4243-95f7-d1b9ff023db4","alertActive":null,"sensitive":null,"startTime":"09:00","endTime":"17:00","contactOutcomeId":"599711cb-2abb-4df9-a8fb-83e5c3b86eea","attendanceData":{"penaltyTime":"01:00","hiVisWorn":true,"workedIntensively":true,"workQuality":"GOOD","behaviour":"POOR"},"enforcementData":{"enforcementActionId":"7ab7f428-f72a-4057-84b1-3342e04264a9","respondBy":"2025-11-12"},"supervisorOfficerCode":"N56A218","notes":"They did a good job","formKeyToDelete":{"id":"9c4a7746-ac7b-4231-8479-f543220e8450","type":"APPOINTMENT_UPDATE_ADMIN"}}"""))
        .check(HttpDsl.status().`in`(200, 201))
    )
    // Cycle 4 - Suspended (CO40)
    .exec(
      HttpDsl.http("PUT form 37d3debd - supervisor only")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/37d3debd-c275-4e58-a849-42a28c44c138")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218"}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("PUT form 37d3debd - Suspended CO40")
        .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/37d3debd-c275-4e58-a849-42a28c44c138")
        .asJson()
        .body(CoreDsl.StringBody("""{"supervisorOfficerCode":"N56A218","contactOutcome":{"id":"bbcaefe1-6ac4-446e-bd12-15883dad3582","name":"Suspended","code":"CO40","enforceable":false,"attended":false,"availableToSupervisors":false},"startTime":"09:00","endTime":"17:00","attendanceData":{"penaltyTime":"01:00"}}"""))
        .check(HttpDsl.status().`in`(200, 201, 204))
    )
    .exec(
      HttpDsl.http("POST appointment outcome - Suspended")
        .post("/admin/appointments/2500629219/outcome")
        .asJson()
        .body(CoreDsl.StringBody("""{"deliusId":2500629219,"deliusVersionToUpdate":"6ab6dade-e96b-4243-95f7-d1b9ff023db4","alertActive":null,"sensitive":null,"startTime":"09:00","endTime":"17:00","contactOutcomeId":"bbcaefe1-6ac4-446e-bd12-15883dad3582","supervisorOfficerCode":"N56A218","formKeyToDelete":{"id":"37d3debd-c275-4e58-a849-42a28c44c138","type":"APPOINTMENT_UPDATE_ADMIN"}}"""))
        .check(HttpDsl.status().`in`(200, 201))
    )

  private val sc = ScenarioConfig.fromEnv()
  init {
    setUp(
      scn.injectOpen(
          CoreDsl.nothingFor(sc.nothingFor),
          CoreDsl.atOnceUsers(sc.atOnceUsers),
          CoreDsl.rampUsers(sc.rampUsers).during(sc.rampUsersDuring),
          CoreDsl.constantUsersPerSec(sc.constantUsersPerSec).during(sc.constantUsersPerSecDuring)
      )
    ).protocols(httpProtocol)
  }
}
package uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import java.time.LocalDate
import java.util.UUID

/**
 * Case Admin - ETE Online Learning Journey
 * Simulates a case admin processing online learning course completions
 */
class CaseAdminOnlineLearningJourneySimulation : BaseSimulationBackEndApi() {

  private val sc = ScenarioConfig.fromEnv()

  private val scn = CoreDsl.scenario("Case Admin - ETE Online Learning Journey")
    .exec(buildScenario())

  init {
    setUp(
      scn.injectOpen(
        CoreDsl.nothingFor(sc.nothingFor),
        CoreDsl.atOnceUsers(sc.atOnceUsers),
        CoreDsl.rampUsers(sc.rampUsers).during(sc.rampUsersDuring),
        CoreDsl.constantUsersPerSec(sc.constantUsersPerSec).during(sc.constantUsersPerSecDuring),
      ),
    ).protocols(httpProtocol)
  }

  companion object {
    private val sc = ScenarioConfig.fromEnv()
    private val objectMapper = ObjectMapper()
      .registerKotlinModule()
      .registerModule(JavaTimeModule())

    fun buildScenario(): ChainBuilder {
      return CoreDsl.feed(CoreDsl.csv("data/ete-course-completions-data.csv").circular())
        .exec { session ->
          session.set("formId", UUID.randomUUID().toString())
        }
        .exec(
          HttpDsl.http("GET Providers")
            .get("/admin/providers?username=${sc.caseAdminUsername}")
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("GET Community Campus PDUs")
            .get("/common/references/community-campus-pdus")
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("GET Course Completions")
            .get("/admin/providers/#{providerCode}/course-completions?pduId=#{pduId}&resolutionStatus=Unresolved&sort=completionDateTime,asc&page=0&size=10")
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("GET Course Completion Details")
            .get("/admin/course-completions/#{courseCompletionId}")
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("PUT Form")
            .put("/common/forms/COURSE_COMPLETION_RESOLUTION/#{formId}")
            .asJson()
            .body(CoreDsl.StringBody("""{"crn":"#{crn}"}"""))
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("GET Offender Summary")
            .get("/admin/offenders/#{crn}/summary")
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec { session ->
          val courseCompletionResolution = CourseCompletionResolutionDto(
            crn = session.getString("crn"),
            type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
            creditTimeDetails = CourseCompletionCreditTimeDetailsDto(
              deliusEventNumber = 1,
              date = LocalDate.now(),
              appointmentIdToUpdate = session.getLong("appointmentId"),
              minutesToCredit = 180,
              contactOutcomeCode = "ATTC",
              projectCode = session.getString("projectCode")!!,
              notes = null,
              alertActive = false,
              sensitive = false,
            ),
            dontCreditTimeDetails = null,
          )
          session.set("courseCompletionResolution", objectMapper.writeValueAsString(courseCompletionResolution))
        }
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("PUT Form")
            .put("/common/forms/COURSE_COMPLETION_RESOLUTION/#{formId}")
            .asJson()
            .body(CoreDsl.StringBody("#{courseCompletionResolution}"))
            .check(HttpDsl.status().`is`(200)),
        )
//        .pause(1.seconds.toJavaDuration())
        .exec(
          HttpDsl.http("POST Course Completion Resolution")
            .post("/admin/course-completions/#{courseCompletionId}/resolution")
            .asJson()
            .body(CoreDsl.StringBody("#{courseCompletionResolution}"))
            .check(HttpDsl.status().`in`(204, 409)),
        )
//        .pause(1.seconds.toJavaDuration())
    }
  }
}

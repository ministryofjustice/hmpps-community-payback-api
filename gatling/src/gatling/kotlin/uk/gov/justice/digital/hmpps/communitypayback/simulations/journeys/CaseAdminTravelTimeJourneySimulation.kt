package uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Case Admin - Travel Time Journey
 * Simulates a case admin processing travel time adjustments for appointments
 */
class CaseAdminTravelTimeJourneySimulation : BaseSimulationBackEndApi() {

  private val sc = ScenarioConfig.fromEnv()

  private val scn = CoreDsl.scenario("Case Admin - Travel Time Journey")
    .exec(buildScenario())
  private val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
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

    fun buildScenario(): ChainBuilder = CoreDsl.feed(CoreDsl.csv("data/travel-time-data.csv").circular())
      .exec(
        HttpDsl.http("GET Providers")
          .get("/admin/providers?username=${sc.caseAdminUsername}")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Pending Appointment Tasks")
          .get("/admin/appointment-tasks/pending")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Appointment Details")
          .get("/admin/projects/#{projectCode}/appointments/#{appointmentId}")
          .check(HttpDsl.status().`is`(200))
          .check(CoreDsl.bodyString().saveAs("appointmentJson")),
      )
      .exitHereIfFailed()
      .pause(1.seconds.toJavaDuration())
      .exec { session ->
        val appointmentJson = session.getString("appointmentJson")
        val appointment = objectMapper.readValue(appointmentJson, AppointmentDto::class.java)
        session.set("crn", appointment.offender.crn)
      }
      .exitHereIfFailed()
      .exec(
        HttpDsl.http("GET Adjustment Reasons")
          .get("/common/references/adjustment-reasons")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec { session ->
        val createAdjustment = CreateAdjustmentDto(
          taskId = UUID.fromString(session.getString("taskId")!!),
          type = CreateAdjustmentTypeDto.Positive,
          adjustmentReasonId = UUID.fromString("a22f1f61-6fe4-4e75-90f4-290639290b2c"),
          minutes = 1,
          dateOfAdjustment = LocalDate.now(),
        )
        session.set("createAdjustment", objectMapper.writeValueAsString(createAdjustment))
      }
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("POST Travel Time Adjustment")
          .post("/admin/offenders/#{crn}/unpaid-work-details/1/adjustments")
          .asJson()
          .body(CoreDsl.StringBody("#{createAdjustment}"))
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
  }
}

package uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentReasonsDto
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
  init {
    setUp(
      scn.injectOpen(
        CoreDsl.nothingFor(sc.nothingFor),
        CoreDsl.atOnceUsers(sc.atOnceUsers),
        CoreDsl.rampUsers(sc.rampUsers).during(sc.rampUsersDuring),
        CoreDsl.constantUsersPerSec(sc.constantUsersPerSec).during(sc.constantUsersPerSecDuring),
      ),
    ).protocols(httpProtocol)
      .assertions(CoreDsl.global().successfulRequests().percent().gt(99.0))
  }

  companion object {
    private val sc = ScenarioConfig.fromEnv()
    private val objectMapper = jacksonObjectMapper()

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
        session.setAll(
          mapOf(
            "crn" to appointment.offender.crn,
            "deliusEventNumber" to appointment.deliusEventNumber,
            "communityPaybackId" to requireNotNull(appointment.communityPaybackId) { "Travel time requires an appointment recorded in Community Payback" }.toString(),
          ),
        )
      }
      .exitHereIfFailed()
      .exec(
        HttpDsl.http("GET Adjustment Reasons")
          .get("/common/references/adjustment-reasons?needsLinkToAppointment=true")
          .check(HttpDsl.status().`is`(200))
          .check(CoreDsl.bodyString().saveAs("adjustmentReasonsJson")),
      )
      .exitHereIfFailed()
      .pause(1.seconds.toJavaDuration())
      .exec { session ->
        val travelTimeReason = objectMapper.readValue(session.getString("adjustmentReasonsJson"), AdjustmentReasonsDto::class.java)
          .adjustmentReasons.single { it.deliusCode == "TTX" }
        val createAdjustment = CreateAdjustmentDto(
          taskId = UUID.fromString(session.getString("taskId")!!),
          type = CreateAdjustmentTypeDto.Positive,
          adjustmentReasonId = travelTimeReason.id,
          minutes = 1,
          adjustmentDate = LocalDate.now(),
          appointmentId = UUID.fromString(session.getString("communityPaybackId")!!),
        )
        session.set("createAdjustment", objectMapper.writeValueAsString(createAdjustment))
      }
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("POST Travel Time Adjustment")
          .post("/admin/offenders/#{crn}/unpaid-work-details/#{deliusEventNumber}/adjustments")
          .asJson()
          .body(CoreDsl.StringBody("#{createAdjustment}"))
          .check(HttpDsl.status().`is`(201)),
      )
      .pause(1.seconds.toJavaDuration())
  }
}

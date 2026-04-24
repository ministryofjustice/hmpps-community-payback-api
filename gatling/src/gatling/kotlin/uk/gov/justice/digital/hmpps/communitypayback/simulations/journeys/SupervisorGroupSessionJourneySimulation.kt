package uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentBehaviourDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentWorkQualityDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Supervisor - Update Outcome Journey
 * Simulates a supervisor updating appointment outcomes for a session
 */
class SupervisorGroupSessionJourneySimulation : BaseSimulationBackEndApi(isSupervisor = true) {

  private val sc = ScenarioConfig.fromEnv()

  private val scn = CoreDsl.scenario("Supervisor - Group Session Journey")
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

    fun buildScenario(): ChainBuilder = CoreDsl.feed(CoreDsl.csv("data/supervisor-group-sessions-data.csv").circular())
      .exec { session ->
        session.set("formId", UUID.randomUUID().toString())
      }
      .exec(
        HttpDsl.http("GET Supervisors")
          .get("/supervisor/supervisors?username=${sc.supervisorUsername}")
          .check(HttpDsl.status().`in`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Future Sessions for Supervisor")
          .get("/supervisor/providers/#{providerCode}/teams/#{teamCode}/sessions/future")
          .check(HttpDsl.status().`in`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Project Session for Supervisor")
          .get("/supervisor/projects/#{projectCode}/sessions/#{sessionDate}")
          .check(HttpDsl.status().`in`(200)),
      )
      .pause(2.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Appointment Details")
          .get("/supervisor/projects/#{projectCode}/appointments/#{appointmentId}")
          .check(HttpDsl.status().`in`(200))
          .check(CoreDsl.bodyString().saveAs("appointmentJson")),
      )
      .pause(2.seconds.toJavaDuration())
      .exitHereIfFailed()
      .exec { session ->
        val appointmentJson = session.getString("appointmentJson")
        val appointment = objectMapper.readValue(appointmentJson, AppointmentDto::class.java)
        val updateAppointmentDto = UpdateAppointmentDto(
          deliusId = appointment.id,
          startTime = appointment.startTime,
          endTime = appointment.endTime,
          deliusVersionToUpdate = appointment.version,
          date = appointment.date,
          supervisorOfficerCode = appointment.supervisorOfficerCode,
          sensitive = appointment.sensitive,
          alertActive = appointment.alertActive,
          attendanceData = AttendanceDataDto(
            hiVisWorn = true,
            workedIntensively = true,
            workQuality = AppointmentWorkQualityDto.entries.toTypedArray().random(),
            behaviour = AppointmentBehaviourDto.entries.toTypedArray().random(),
          ),
          contactOutcomeCode = appointment.contactOutcomeCode,
        )
        session.set("updateAppointmentJson", objectMapper.writeValueAsString(updateAppointmentDto))
      }
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("PUT Form")
          .put("/common/forms/APPOINTMENT_UPDATE_SUPERVISOR/#{formId}")
          .asJson()
          .body(CoreDsl.StringBody("#{updateAppointmentJson}"))
          .check(HttpDsl.status().`in`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exitHereIfFailed()
      .exec(
        HttpDsl.http("GET Contact Outcomes")
          .get("/common/references/contact-outcomes")
          .check(HttpDsl.status().`in`(200)),
      )
      .exitHereIfFailed()
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("PUT Appointment for Supervisor")
          .put("/supervisor/projects/#{projectCode}/appointments/#{appointmentId}")
          .asJson()
          .body(CoreDsl.StringBody("#{updateAppointmentJson}"))
          .check(HttpDsl.status().`in`(200, 409)),
      )
      .exitHereIfFailed()
      .pause(1.seconds.toJavaDuration())
  }
}

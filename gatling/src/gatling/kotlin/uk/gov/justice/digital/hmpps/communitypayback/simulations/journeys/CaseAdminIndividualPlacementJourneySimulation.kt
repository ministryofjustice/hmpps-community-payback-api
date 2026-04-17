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
 * Case Admin - Individual Placement Journey
 * Simulates a case admin managing individual placement appointments
 */
class CaseAdminIndividualPlacementJourneySimulation : BaseSimulationBackEndApi() {

  private val sc = ScenarioConfig.fromEnv()

  private val scn = CoreDsl.scenario("Case Admin - Individual Placement Journey")
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

    fun buildScenario(): ChainBuilder = CoreDsl.feed(CoreDsl.csv("data/individual-placements-data.csv").circular())
      .exec { session ->
        session.set("formId", UUID.randomUUID().toString())
      }
      .exec(
        HttpDsl.http("GET Providers")
          .get("/admin/providers?username=${sc.caseAdminUsername}")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Individual Projects")
          .get("/admin/providers/#{providerCode}/teams/#{teamCode}/projects?projectTypeGroup=INDIVIDUAL&overdueDays=45")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Project Details")
          .get("/admin/projects/#{projectCode}")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Appointments without an Outcome")
          .get("/admin/appointments?projectCodes=#{projectCode}&outcomeCodes=NO_OUTCOME&toDate=2026-04-20&fromDate=2026-03-06")
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("GET Appointment Details")
          .get("/admin/projects/#{projectCode}/appointments/#{appointmentId}")
          .check(HttpDsl.status().`is`(200))
          .check(CoreDsl.bodyString().saveAs("appointmentJson")),
      )
      .pause(1.seconds.toJavaDuration())
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
      .exec(
        HttpDsl.http("PUT Form")
          .put("/common/forms/APPOINTMENT_UPDATE_ADMIN/#{formId}")
          .asJson()
          .body(CoreDsl.StringBody("#{updateAppointmentJson}"))
          .check(HttpDsl.status().`is`(200)),
      )
      .pause(1.seconds.toJavaDuration())
      .exec(
        HttpDsl.http("PUT Appointment")
          .put("/admin/projects/#{projectCode}/appointments/#{appointmentId}")
          .asJson()
          .body(CoreDsl.StringBody("#{updateAppointmentJson}"))
          .check(HttpDsl.status().`in`(200, 409)),
      )
      .pause(1.seconds.toJavaDuration())
  }
}

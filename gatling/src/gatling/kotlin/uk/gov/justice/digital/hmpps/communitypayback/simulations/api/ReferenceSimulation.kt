package uk.gov.justice.digital.hmpps.communitypayback.simulations.api

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.nothingFor
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi

class ReferenceSimulation(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = scenario("Project Types Endpoints")
    .exec(
      http("Project Types Endpoint")
        .get("/common/references/project-types")
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

    .exec(
      http("Contact outcomes Endpoint")
        .get("/common/references/contact-outcomes")
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

    .exec(
      http("Enforcement Actions Endpoint")
        .get("/common/references/enforcement-actions")
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

  val sc = ScenarioConfig.fromEnv()
  init {
    setUp(
      scn.injectOpen(
        nothingFor(sc.nothingFor),
        atOnceUsers(sc.atOnceUsers),
        rampUsers(sc.rampUsers).during(sc.rampUsersDuring),
        constantUsersPerSec(sc.constantUsersPerSec).during(sc.constantUsersPerSecDuring)
      )
    ).protocols(httpProtocol)
  }
}

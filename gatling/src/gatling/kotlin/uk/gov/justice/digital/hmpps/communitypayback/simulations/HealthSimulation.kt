package uk.gov.justice.digital.hmpps.communitypayback.simulations

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.nothingFor
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi

class HealthSimulation(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = scenario("Health Endpoints")
    .exec(
      http("Health Endpoint")
        .get("/health")
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

  init {
    setUp(
      scn.injectOpen(
        nothingFor(5L), // Wait for 5 seconds
        atOnceUsers(10), // Start with 10 users at once
        rampUsers(50).during(30L), // Ramp up to 50 users over 30 seconds
        constantUsersPerSec(10.0).during(60L) // Maintain 10 users/sec for 60 seconds
      )
    ).protocols(httpProtocol)
  }
}

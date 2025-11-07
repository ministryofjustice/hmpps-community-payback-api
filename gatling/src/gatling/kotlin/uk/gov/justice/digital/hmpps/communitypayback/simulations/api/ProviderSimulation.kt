package uk.gov.justice.digital.hmpps.communitypayback.simulations.api

import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.nothingFor
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi

class ProviderSimulation(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = scenario("Provider Endpoints")
    .exec(
      http("Provider Summary Endpoint")
        .get("/admin/providers")
        .check(
          status().`is`(200),
          jsonPath("$.providers[0].id").saveAs("providerId"),
          bodyString().saveAs("responseBody")
        )
    )

    .exec(
      http("Provider Team Endpoint")
        .get { session ->
          val providerId = session.getString("providerId")
          jsonPath("$.providers[0].id").saveAs("teamId")
          "/admin/providers/$providerId/teams"
        }
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json"),
    )

    .exec(
      http("Provider Supervisor Endpoint")
        .get { session ->
          val providerId = session.getString("providerId")
          val teamId = session.getString("teamId")
          "/admin/providers/$providerId/teams/$teamId/supervisors"
        }
        .check(
          status().`is`(200),
          bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json"),
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

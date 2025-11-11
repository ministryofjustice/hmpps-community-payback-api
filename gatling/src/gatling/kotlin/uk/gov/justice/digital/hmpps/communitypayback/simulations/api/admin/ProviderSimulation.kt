package uk.gov.justice.digital.hmpps.communitypayback.simulations.api.admin

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig

class ProviderSimulation(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = CoreDsl.scenario("Provider Endpoints")
    .exec(
      HttpDsl.http("Provider Summary Endpoint")
        .get("/admin/providers")
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.jsonPath("$.providers[0].id").saveAs("providerId"),
          CoreDsl.bodyString().saveAs("responseBody")
        )
    )

    .exec(
      HttpDsl.http("Provider Team Endpoint")
        .get { session ->
          val providerId = session.getString("providerId")
          CoreDsl.jsonPath("$.providers[0].id").saveAs("teamId")
          "/admin/providers/$providerId/teams"
        }
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json"),
    )

    .exec(
      HttpDsl.http("Provider Supervisor Endpoint")
        .get { session ->
          val providerId = session.getString("providerId")
          val teamId = session.getString("teamId")
          "/admin/providers/$providerId/teams/$teamId/supervisors"
        }
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json"),
    )

  val sc = ScenarioConfig.Companion.fromEnv()
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
package uk.gov.justice.digital.hmpps.communitypayback.simulations.api.common

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.config.ScenarioConfig

class ReferenceSimulation(
) : BaseSimulationBackEndApi() {
  // Scenario definition
  private val scn = CoreDsl.scenario("Project Types Endpoints")
    .exec(
      HttpDsl.http("Project Types Endpoint")
        .get("/common/references/project-types")
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

    .exec(
      HttpDsl.http("Contact outcomes Endpoint")
        .get("/common/references/contact-outcomes")
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
    )

    .exec(
      HttpDsl.http("Enforcement Actions Endpoint")
        .get("/common/references/enforcement-actions")
        .check(
          HttpDsl.status().`is`(200),
          CoreDsl.bodyString().saveAs("responseBody")
        )
        .header("Content-Type", "application/json")
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
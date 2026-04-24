package uk.gov.justice.digital.hmpps.communitypayback.simulations

import io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import uk.gov.justice.digital.hmpps.communitypayback.BaseSimulationBackEndApi
import uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminGroupSessionsJourneySimulation
import uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminIndividualPlacementJourneySimulation
import uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminOnlineLearningJourneySimulation
import uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminTravelTimeJourneySimulation
import uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.SupervisorGroupSessionJourneySimulation
import java.time.Duration

class CommunityPaybackUserJourneysSimulation : BaseSimulationBackEndApi() {

  private val caseAdminGroupSessionsScenario = scenario("Case Admin Group Sessions Journey")
    .exec(CaseAdminGroupSessionsJourneySimulation.buildScenario())

  private val individualPlacementScenario = scenario("Individual Placement Journey")
    .exec(CaseAdminIndividualPlacementJourneySimulation.buildScenario())

  private val eteOnlineLearningScenario = scenario("ETE Online Learning Journey")
    .exec(CaseAdminOnlineLearningJourneySimulation.buildScenario())

  private val travelTimeScenario = scenario("Travel Time Journey")
    .exec(CaseAdminTravelTimeJourneySimulation.buildScenario())

  private val supervisorGroupSessionScenario = scenario("Supervisor Group Session Journey")
    .exec(SupervisorGroupSessionJourneySimulation.buildScenario())

  init {
    setUp(
      caseAdminGroupSessionsScenario.injectClosed(
        rampConcurrentUsers(0).to(20).during(Duration.ofMinutes(3)),
        constantConcurrentUsers(20).during(Duration.ofMinutes(5)),
      ),
      individualPlacementScenario.injectClosed(
        rampConcurrentUsers(0).to(5).during(Duration.ofMinutes(3)),
        constantConcurrentUsers(5).during(Duration.ofMinutes(5)),
      ),
      eteOnlineLearningScenario.injectClosed(
        rampConcurrentUsers(0).to(5).during(Duration.ofMinutes(3)),
        constantConcurrentUsers(5).during(Duration.ofMinutes(5)),
      ),
      travelTimeScenario.injectClosed(
        rampConcurrentUsers(0).to(5).during(Duration.ofMinutes(3)),
        constantConcurrentUsers(5).during(Duration.ofMinutes(5)),
      ),
      supervisorGroupSessionScenario.injectClosed(
        rampConcurrentUsers(0).to(25).during(Duration.ofMinutes(3)),
        constantConcurrentUsers(25).during(Duration.ofMinutes(5)),
      ).protocols(supervisorHttpProtocol),
    ).protocols(httpProtocol)
      .assertions(
        global().responseTime().percentile(95.0).lt(5000),
        global().successfulRequests().percent().gt(99.0),
      )
  }
}

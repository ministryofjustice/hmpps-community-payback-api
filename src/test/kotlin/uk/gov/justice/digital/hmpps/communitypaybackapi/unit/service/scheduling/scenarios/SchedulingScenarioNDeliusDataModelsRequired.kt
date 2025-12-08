package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Indicates that a scenario can't be implemented and the logic is instead covered by the
 * [uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.SchedulingMappersTest]
 *
 * This is because the scenario is testing that the correct values are derived from the NDelius
 * data models when building the internal scheduling data model, e.g. the correct end date or frequency
 * is derived from the various NDelius data models
 *
 * Core scheduling tests currently operate on the code that makes use of these derived models directly.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Disabled
@Test
internal annotation class SchedulingScenarioNDeliusDataModelsRequired

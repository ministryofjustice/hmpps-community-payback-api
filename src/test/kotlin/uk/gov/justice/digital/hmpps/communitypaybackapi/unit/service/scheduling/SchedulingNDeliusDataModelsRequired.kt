package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Indicates that a scenario can't currently be implemented
 *
 * This is typically because the scenario is testing that the correct
 * values are derived from the NDelius data models when building the
 * internal scheduling data model, e.g. the correct end date or frequency
 * is derived from the various NDelius data models
 *
 * Core scheduling tests currently operate on the code that makes use of these derived
 * models directly.
 *
 * These scenarios can be implemented once we've added the upstream NDelius
 * data models into the code, at which point we'll update core scenarios to
 * operate with these data models instead
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Disabled
@Test
internal annotation class SchedulingNDeliusDataModelsRequired

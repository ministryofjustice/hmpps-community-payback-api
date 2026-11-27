package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AutomatedTravelTimeService

class AutomatedTravelTimeDisabledIT : IntegrationTestBase() {

  @Test
  fun `service is not registered when the feature is disabled`() {
    assertThat(ctx.getBeanProvider(AutomatedTravelTimeService::class.java).ifAvailable).isNull()
  }
}

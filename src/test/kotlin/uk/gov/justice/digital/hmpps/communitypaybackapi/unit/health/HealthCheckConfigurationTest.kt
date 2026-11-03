package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.health.ArnsHealthPing
import uk.gov.justice.digital.hmpps.communitypaybackapi.health.CommunityPaybackAndDeliusHealthPing
import uk.gov.justice.digital.hmpps.communitypaybackapi.health.HmppsAuthHealthPing
import uk.gov.justice.digital.hmpps.communitypaybackapi.health.ProbationAccessControlHealthPing
import uk.gov.justice.digital.hmpps.communitypaybackapi.health.ProbationOffenderSearchHealthPing

class HealthCheckConfigurationTest {
  private val healthChecks = mapOf(
    "arns" to ArnsHealthPing::class.java,
    "communityPaybackAndDelius" to CommunityPaybackAndDeliusHealthPing::class.java,
    "hmppsAuth" to HmppsAuthHealthPing::class.java,
    "probationAccessControl" to ProbationAccessControlHealthPing::class.java,
    "probationOffenderSearch" to ProbationOffenderSearchHealthPing::class.java,
  )

  private val contextRunner = ApplicationContextRunner()
    .withUserConfiguration(*healthChecks.values.toTypedArray())
    .withBean("arnsWebClient", WebClient::class.java, { WebClient.create() })
    .withBean("communityPaybackAndDeliusWebClient", WebClient::class.java, { WebClient.create() })
    .withBean("hmppsAuthHealthWebClient", WebClient::class.java, { WebClient.create() })
    .withBean("probationAccessControlWebClient", WebClient::class.java, { WebClient.create() })
    .withBean("probationOffenderSearchWebClient", WebClient::class.java, { WebClient.create() })

  @Test
  fun `all downstream health checks are enabled by default`() {
    contextRunner.run { context ->
      healthChecks.values.forEach { assertThat(context).hasSingleBean(it) }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["arns", "communityPaybackAndDelius", "hmppsAuth", "probationAccessControl", "probationOffenderSearch"])
  fun `disabling a health check leaves other downstream checks enabled`(disabledCheck: String) {
    contextRunner.withPropertyValues("management.health.$disabledCheck.enabled=false").run { context ->
      healthChecks.forEach { (name, type) ->
        if (name == disabledCheck) {
          assertThat(context).doesNotHaveBean(type)
        } else {
          assertThat(context).hasSingleBean(type)
        }
      }
    }
  }

  @Test
  fun `all downstream health checks can be disabled together`() {
    contextRunner.withPropertyValues(*healthChecks.keys.map { "management.health.$it.enabled=false" }.toTypedArray()).run { context ->
      healthChecks.values.forEach { assertThat(context).doesNotHaveBean(it) }
    }
  }
}

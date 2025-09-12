package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

/**
 * Baseline configuration is provided by the hmpps kotlin library, see https://github.com/ministryofjustice/hmpps-kotlin-lib/blob/main/readme-contents/SpringResourceServer.md
 *
 * This file provides additional configuration
 */
@Configuration
class SecurityConfiguration {

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/mocks/community-payback-and-delius/**",
      )
    }
  }
}

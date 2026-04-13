package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

/**
 * Baseline configuration is provided by the hmpps kotlin library, see https://github.com/ministryofjustice/hmpps-kotlin-lib/blob/main/readme-contents/SpringResourceServer.md
 */
object SecurityConfiguration {
  const val ROLE_ADMIN_UI = "ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"
  const val ROLE_DOMAIN_EVENT_DETAILS = "ROLE_COMMUNITY_PAYBACK__DOMAIN_EVENT_DETAILS__ALL__RO"
  const val ROLE_SUPERVISOR_UI = "ROLE_COMMUNITY_PAYBACK__SUPERVISOR"
}

@Configuration
class ResourceServerConfiguration {

  /**
   * This modifies the baseline security configuration](https://github.com/ministryofjustice/hmpps-kotlin-lib/blob/main/readme-contents/SpringResourceServer.md)
   * provided by hmpps-kotlin-lib to exclude the 'retry-all-dlqs' endpoint from security checks,
   * required to enable the [retry cronjob](https://github.com/ministryofjustice/hmpps-helm-charts/tree/main/charts/generic-service#retrying-messages-on-a-dead-letter-queue)
   *
   * The configuration follows the example in the [sqs test app](https://github.com/ministryofjustice/hmpps-spring-boot-sqs/blob/main/test-app/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppstemplatepackagename/config/ResourceServerConfiguration.kt)
   *
   * This endpoint _is_ protected at the ingress level (see values.yaml), so the only allows calls must
   * originate from within k8s itself
   */
  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    authorizeHttpRequests {
      listOf(
        "/webjars/**",
        "/favicon.ico",
        "/health/**",
        "/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/queue-admin/retry-all-dlqs",
      ).forEach { authorize(it, permitAll) }
      authorize(anyRequest, authenticated)
    }
  }
}

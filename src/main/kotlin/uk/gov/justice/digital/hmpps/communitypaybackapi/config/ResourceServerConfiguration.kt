package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter

/**
 * The initial version of this file was taken from
 * https://github.com/ministryofjustice/hmpps-spring-boot-sqs/blob/main/test-app,
 * enabling the retry-all-dlq endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain = http {
    headers { frameOptions { sameOrigin = true } }
    sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
    // Can't have CSRF protection as requires session
    csrf { disable() }
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
        "/test-support/**",
      ).forEach { authorize(it, permitAll) }
      authorize(anyRequest, authenticated)
    }
    oauth2ResourceServer { jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() } }
  }.let { http.build() }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${hmpps-auth.health-timeout:2s}") val healthTimeout: Duration,

  @param:Value("\${client.community-payback-and-delius.url}") val communityPaybackAndDeliusUrl: String,
  @param:Value("\${client.community-payback-and-delius.timeout:5s}") val communityPaybackAndDeliusTimeout: Duration,

  @param:Value("\${client.arns.url}") val arnsUrl: String,
  @param:Value("\${client.arns.timeout:5s}") val arnsTimeout: Duration,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  // When configuring this for the actual endpoint we should configure authorisation via builder.authorisedWebClient
  @Bean
  @RequestScope
  fun communityPaybackAndDeliusWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(communityPaybackAndDeliusUrl)
    .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(communityPaybackAndDeliusTimeout)))
    .build()

  @Bean
  @DependsOn("communityPaybackAndDeliusWebClient")
  fun communityPaybackAndDeliusClient(communityPaybackAndDeliusWebClient: WebClient): CommunityPaybackAndDeliusClient = HttpServiceProxyFactory
    .builderFor(WebClientAdapter.create(communityPaybackAndDeliusWebClient))
    .build()
    .createClient(CommunityPaybackAndDeliusClient::class.java)

  @Bean
  @RequestScope
  fun arnsWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder
    .authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "arns",
      url = arnsUrl,
      timeout = arnsTimeout,
    )

  @Bean
  @DependsOn("arnsWebClient")
  fun arnsClient(arnsWebClient: WebClient): ArnsClient = HttpServiceProxyFactory
    .builderFor(WebClientAdapter.create(arnsWebClient))
    .build()
    .createClient(ArnsClient::class.java)
}

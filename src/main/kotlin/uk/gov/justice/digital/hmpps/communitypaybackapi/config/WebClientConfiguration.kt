package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProbationAccessControlClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${hmpps-auth.health-timeout:2s}") val healthTimeout: Duration,

  @param:Value("\${client.community-payback-and-delius.url}") val communityPaybackAndDeliusUrl: String,
  /**
   * Before changing timeouts consider the impact on [DomainEventListener.MESSAGE_VISIBILITY_TIMEOUT]
   */
  @param:Value("\${client.community-payback-and-delius.timeout:5s}") val communityPaybackAndDeliusTimeout: Duration,

  @param:Value("\${client.arns.url}") val arnsUrl: String,
  @param:Value("\${client.arns.timeout:5s}") val arnsTimeout: Duration,

  @param:Value("\${client.probation-access-control.url}") val probationAccessControlUrl: String,
  @param:Value("\${client.probation-access-control.timeout:5s}") val probationAccessControlTimeout: Duration,

  @param:Value("\${client.log-downstream-error-responses:false}") val logDownstreamErrorResponses: Boolean,
) {

  companion object {
    const val API_CLIENT_ID: String = "api-client"
    const val DEFAULT_RETRY_MAX_ATTEMPTS: Long = 3
    val DEFAULT_RETRY_BACKOFF_DURATION: Duration = Duration.ofMillis(100)
  }

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun communityPaybackAndDeliusWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder
    .authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = API_CLIENT_ID,
      url = communityPaybackAndDeliusUrl,
      timeout = communityPaybackAndDeliusTimeout,
    )
    .retryGet(DEFAULT_RETRY_MAX_ATTEMPTS, DEFAULT_RETRY_BACKOFF_DURATION)
    .logErrorResponses<CommunityPaybackAndDeliusClient>(logDownstreamErrorResponses)

  @Bean
  @DependsOn("communityPaybackAndDeliusWebClient")
  fun communityPaybackAndDeliusClient(communityPaybackAndDeliusWebClient: WebClient): CommunityPaybackAndDeliusClient = HttpServiceProxyFactory
    .builderFor(WebClientAdapter.create(communityPaybackAndDeliusWebClient))
    .build()
    .createClient<CommunityPaybackAndDeliusClient>()

  @Bean
  fun arnsWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder
    .authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = API_CLIENT_ID,
      url = arnsUrl,
      timeout = arnsTimeout,
    )
    .retryGet(DEFAULT_RETRY_MAX_ATTEMPTS, DEFAULT_RETRY_BACKOFF_DURATION)
    .logErrorResponses<ArnsClient>(logDownstreamErrorResponses)

  @Bean
  @DependsOn("arnsWebClient")
  fun arnsClient(arnsWebClient: WebClient): ArnsClient = HttpServiceProxyFactory
    .builderFor(WebClientAdapter.create(arnsWebClient))
    .build()
    .createClient<ArnsClient>()

  @Bean
  fun probationAccessControlWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder
    .authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = API_CLIENT_ID,
      url = probationAccessControlUrl,
      timeout = probationAccessControlTimeout,
    )
    .retryGet(DEFAULT_RETRY_MAX_ATTEMPTS, DEFAULT_RETRY_BACKOFF_DURATION)
    .logErrorResponses<ProbationAccessControlClient>(logDownstreamErrorResponses)

  @Bean
  @DependsOn("probationAccessControlWebClient")
  fun probationAccessControlClient(probationAccessControlWebClient: WebClient): ProbationAccessControlClient = HttpServiceProxyFactory
    .builderFor(WebClientAdapter.create(probationAccessControlWebClient))
    .build()
    .createClient<ProbationAccessControlClient>()
}

inline fun <reified T> WebClient.logErrorResponses(enabled: Boolean): WebClient {
  if (!enabled) {
    return this
  }

  val logger = LoggerFactory.getLogger(T::class.java)

  return this.mutate()
    .filter(
      ExchangeFilterFunction.ofResponseProcessor { response ->
        response.bodyToMono<String>().defaultIfEmpty("").map { body ->
          if (response.statusCode().isError) {
            logger.warn("Downstream API returned ${response.statusCode()}: ${body.ifEmpty { "<empty>" }}")
          } else {
            logger.debug("Downstream API returned successful response")
          }

          response.mutate().body(body).build()
        }
      },
    )
    .build()
}

fun WebClient.retryGet(maxAttempts: Long, backoff: Duration): WebClient = this.mutate()
  .filter { request, next ->
    val responseMono = next.exchange(request)
    if (request.method() == HttpMethod.GET) {
      responseMono.retryWhen(
        Retry.backoff(maxAttempts, backoff)
          .filter { it.isTimeoutException() },
      )
    } else {
      responseMono
    }
  }
  .build()

private fun Throwable.isTimeoutException(): Boolean = this.hasExactCauseType<ReadTimeoutException>() ||
  this.hasExactCauseType<ConnectTimeoutException>()

private inline fun <reified T : Throwable> Throwable.hasExactCauseType(): Boolean {
  var current: Throwable? = this
  while (current != null) {
    if (current.javaClass == T::class.java) {
      return true
    }
    current = current.cause
  }
  return false
}

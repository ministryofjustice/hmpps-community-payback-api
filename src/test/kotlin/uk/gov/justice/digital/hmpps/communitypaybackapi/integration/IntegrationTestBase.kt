package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.wiremock.spring.EnableWireMock
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.container.PostgresContainer.setPostgresProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("integrationtest")
@EnableWireMock
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @BeforeEach
  fun before() {
    // this provides an oauth token for any calls to upstream APIs
    HmppsAuthMockServer.stubGrantToken()
  }

  companion object {
    private val localStackContainer = LocalStackContainer.instance
    private val postgresContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      System.setProperty("aws.region", "eu-west-2")

      // Set LocalStack properties if managed by testcontainers
      localStackContainer?.also { setLocalStackProperties(it, registry) }

      // Set Postgres datasource properties
      postgresContainer?.also { setPostgresProperties(it, registry) }
    }
  }

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    HmppsAuthMockServer.stubHealthPing(status)
  }

  fun <S : WebTestClient.RequestHeadersSpec<S>> S.addUiAuthHeader(username: String = "AUTH_ADM"): S = this.headers(
    setAuthorisation(
      username = username,
      roles = listOf(SecurityConfiguration.ROLE_UI),
    ),
  ) as S
}

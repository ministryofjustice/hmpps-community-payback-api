package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

class ResourceSecurityTest : IntegrationTestBase() {

  @Autowired
  private lateinit var context: ApplicationContext

  private companion object {
    private val unprotectedEndpoints = setOf(
      Endpoint(RequestMethod.GET, "/v3/api-docs.yaml"),
      Endpoint(RequestMethod.GET, "/swagger-ui.html"),
      Endpoint(RequestMethod.GET, "/v3/api-docs"),
      Endpoint(method = RequestMethod.GET, path = "/v3/api-docs/swagger-config"),
      Endpoint(RequestMethod.GET, "/queue-admin/retry-all-dlqs"),
      Endpoint(RequestMethod.GET, "/mocks/community-payback-and-delius/providers"),
      Endpoint(RequestMethod.GET, "/mocks/community-payback-and-delius/provider-teams"),
    )
  }

  @Test
  fun `should return unauthorized if no token provided for all secured endpoints`() {
    forEachEndpoint { httpMethod, uri ->
      webTestClient
        .method(httpMethod)
        .uri(uri)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Test
  fun `should return forbidden if no role for all secured endpoints`() {
    forEachEndpoint { httpMethod, uri ->
      webTestClient
        .method(httpMethod)
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = emptyList()))
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `should return forbidden if wrong role for all secured endpoints`() {
    forEachEndpoint { httpMethod, uri ->
      webTestClient
        .method(httpMethod)
        .uri(uri)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  private fun forEachEndpoint(
    action: (httpMethod: HttpMethod, uri: String) -> Unit,
  ) {
    val exclusions = externalAccessAlreadyBlockedByNginx() + unprotectedEndpoints
    val endpoints = findApiEndpoints()

    endpoints
      .filter { endpoint -> !exclusions.contains(endpoint) }
      .forEach { endpoint ->
        val httpMethod = endpoint.method.toHttpMethod()
        val uri = endpoint.path.replace(Regex("\\{[^}]*}"), "1234")

        action.invoke(httpMethod, uri)
      }
  }

  private fun findApiEndpoints(): List<Endpoint> {
    val handlerMappings = context.getBeansOfType(RequestMappingHandlerMapping::class.java).values
    val requestMappingInfos = handlerMappings.flatMap { it.handlerMethods.map { (key, _) -> key } }

    return requestMappingInfos.flatMap { requestMappingInfo ->
      requestMappingInfo.methodsCondition.methods.flatMap { method ->
        requestMappingInfo.patternValues.map { pattern -> Endpoint(method, pattern) }
      }
    }.sortedBy { it.path }
  }

  private fun externalAccessAlreadyBlockedByNginx() = File("helm_deploy").walk()
    .filter { it.name.equals("values.yaml") }
    .flatMap { file ->
      file.readLines().map { line ->
        line.takeIf { it.contains("location") }?.substringAfter("location ")?.substringBefore(" {")
      }
    }
    .filterNotNull()
    .flatMap { path -> RequestMethod.entries.map { method -> Endpoint(method, path) } }
    .toMutableSet()

  private fun RequestMethod.toHttpMethod() = when (this) {
    RequestMethod.GET -> HttpMethod.GET
    RequestMethod.HEAD -> HttpMethod.HEAD
    RequestMethod.POST -> HttpMethod.POST
    RequestMethod.PUT -> HttpMethod.PUT
    RequestMethod.PATCH -> HttpMethod.PATCH
    RequestMethod.DELETE -> HttpMethod.DELETE
    RequestMethod.OPTIONS -> HttpMethod.OPTIONS
    RequestMethod.TRACE -> HttpMethod.TRACE
  }

  private data class Endpoint(val method: RequestMethod, val path: String)
}

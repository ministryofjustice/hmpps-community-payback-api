package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.File

class ResourceSecurityIT : IntegrationTestBase() {
  @Autowired
  private lateinit var context: ApplicationContext

  private val unprotectedDefaultMethods = setOf(
    "GET /v3/api-docs.yaml",
    "GET /swagger-ui.html",
    "GET /v3/api-docs",
    "GET /v3/api-docs/swagger-config",
    "GET /queue-admin/retry-all-dlqs",
    " /error",
    "GET /mocks/community-payback-and-delius/providers",
    "GET /mocks/community-payback-and-delius/provider-teams",
    "GET /mocks/community-payback-and-delius/project-allocations",
    "GET /mocks/community-payback-and-delius/references/project-types",
  )

  @Test
  fun `Ensure all endpoints protected with PreAuthorize`() {
    // need to exclude any that are forbidden in helm configuration
    val exclusions = File("helm_deploy").walk()
      .filter { it.name.equals("values.yaml") }
      .flatMap { file ->
        file.readLines().map { line ->
          line.takeIf { it.contains("location") }?.substringAfter("location ")?.substringBefore(" {")
        }
      }
      .filterNotNull()
      .flatMap { path -> listOf("GET", "POST", "PUT", "DELETE").map { method -> "$method $path" } }
      .toMutableSet()
      .also { it.addAll(unprotectedDefaultMethods) }

    val beans = context.getBeansOfType(RequestMappingHandlerMapping::class.java)

    val unprotected = beans.values.asSequence()
      .flatMap { mapping -> mapping.handlerMethods.asSequence() }
      .filter { (_, method) ->
        AnnotationUtils.findAnnotation(method.beanType, PreAuthorize::class.java) == null &&
          AnnotationUtils.findAnnotation(method.method, PreAuthorize::class.java) == null
      }
      .flatMap { (mappingInfo, _) -> mappingInfo.getMappings().asSequence() }
      .filter { mappingStr -> mappingStr !in exclusions }
      .toList()

    assertThat(unprotected).withFailMessage {
      buildString {
        append("Found unsecured endpoints (no PreAuthorize):\n")
        unprotected.forEach { append(" - ").append(it).append('\n') }
      }
    }.isEmpty()
  }
}

private fun RequestMappingInfo.getMappings() = methodsCondition.methods
  .map { it.name }
  .ifEmpty { listOf("") } // if no methods defined then match all rather than none
  .flatMap { method ->
    pathPatternsCondition?.patternValues?.map { "$method $it" } ?: emptyList()
  }

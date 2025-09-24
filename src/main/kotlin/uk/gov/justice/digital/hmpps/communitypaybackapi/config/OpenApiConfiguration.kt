package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  companion object {
    const val SECURITY_SCHEME_UI = "community-payback-ui"
    const val SECURITY_SCHEME_DOMAIN_EVENT_DETAILS = "domain-event-details"
  }

  @Bean
  fun customOpenAPI() = OpenAPI()
    .servers(
      listOf(
        Server().url("https://community-payback-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://community-payback-api-test.hmpps.service.justice.gov.uk").description("Test"),
        Server().url("https://community-payback-api-preprod.hmpps.service.justice.gov.uk")
          .description("Pre-Production"),
        Server().url("https://community-payback-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(),
    )
    .info(
      Info().title("HMPPS Community Payback Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components()
        .addSecuritySchemes(
          SECURITY_SCHEME_UI,
          SecurityScheme().addBearerJwtRequirement(SecurityConfiguration.ROLE_UI),
        )
        .addSecuritySchemes(
          "domain-event-details",
          SecurityScheme().addBearerJwtRequirement(SecurityConfiguration.ROLE_DOMAIN_EVENT_DETAILS),
        ),
    )

  @Bean
  fun allEndpoints(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("All")
    .displayName("All Endpoints")
    .addOpenApiCustomizer(defaultErrorResponseCustomizer())
    .build()

  @Bean
  fun forCommunityPaybackUI(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("ForCommunityPaybackUI")
    .displayName("For Community Payback UI")
    .pathsToExclude("/queue-admin/**")
    .addOpenApiCustomizer(defaultErrorResponseCustomizer())
    .build()

  @Bean
  fun forDomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("DomainEventDetails")
    .displayName("Domain Event Details")
    .pathsToMatch("/domain-event-details/**")
    .addOpenApiCustomizer(defaultErrorResponseCustomizer())
    .build()

  private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT")
    .`in`(SecurityScheme.In.HEADER)
    .name("Authorization")
    .description("A HMPPS Auth access token with the `$role` role.")

  /**
   * Adds 401, 403 and 500 error response structures to all endpoints, aligned
   * with the responses returned by [CommunityPaybackApiExceptionHandler]
   */
  @Bean
  fun defaultErrorResponseCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
    checkNotNull(openApi.components.schemas.keys.firstOrNull { it == "ErrorResponse" }) {
      """/components/schemas/ErrorResponse must exist so it can be referred to for
        for default error responses. Ensure at least one API is defined in code that
         explicitly references the `ErrorResponse` type
      """.trimMargin()
    }

    openApi.paths.values.forEach { pathItem ->
      pathItem.readOperations().forEach { operation ->
        val responses = operation.responses

        addDefaultErrorResponse(responses, "401", "Not authenticated")
        addDefaultErrorResponse(responses, "403", "Unauthorized")
        addDefaultErrorResponse(responses, "500", "Unexpected error")
      }
    }
  }

  private fun addDefaultErrorResponse(
    responses: MutableMap<String, ApiResponse>,
    code: String,
    description: String,
  ) {
    if (!responses.containsKey(code)) {
      responses[code] = ApiResponse()
        .description(description)
        .content(
          Content().addMediaType(
            "application/json",
            MediaType().schema(createErrorSchemaRef()),
          ),
        )
    }
  }

  private fun createErrorSchemaRef(): Schema<*> {
    val schema = Schema<Any>()
    schema.`$ref` = "ErrorResponse"
    return schema
  }
}

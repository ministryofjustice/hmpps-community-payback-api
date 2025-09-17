package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.util.RefUtils
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

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
      Components().addSecuritySchemes(
        "community-payback-ui",
        SecurityScheme().addBearerJwtRequirement("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
      ),
    )

  private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT")
    .`in`(SecurityScheme.In.HEADER)
    .name("Authorization")
    .description("A HMPPS Auth access token with the `$role` role.")

  @Bean
  fun errorResponsesCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
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
            MediaType().schema(createProblemSchema()),
          ),
        )
    }
  }

  /**
   * Taken from https://github.com/swagger-api/swagger-core/issues/3411
   * Ensures discriminator mappings are provided in the open api spec.
   * Used by [uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto]
   */
  @Bean
  fun discriminatorMappingResolver(objectMapper: ObjectMapper?) = object : ModelResolver(objectMapper) {
    override fun resolveDiscriminator(
      type: JavaType?,
      context: ModelConverterContext?,
    ): Discriminator? {
      val discriminator = super.resolveDiscriminator(type, context)
      if (context != null &&
        type != null &&
        discriminator.hasPropertyButNoMapping()
      ) {
        val jsonSubTypes = type.rawClass.getDeclaredAnnotation(JsonSubTypes::class.java)
        jsonSubTypes?.value?.forEach { subtype: JsonSubTypes.Type ->
          discriminator.mapping(
            subtype.name,
            RefUtils.constructRef(context.resolve(AnnotatedType(subtype.value.java)).name),
          )
        }
      }
      return discriminator
    }
  }

  private fun Discriminator?.hasPropertyButNoMapping() = this != null && propertyName != null && (mapping == null || mapping.isEmpty())

  private fun createProblemSchema(): Schema<*> = ModelConverters.getInstance()
    .resolveAsResolvedSchema(AnnotatedType(ErrorResponse::class.java))
    .schema
}

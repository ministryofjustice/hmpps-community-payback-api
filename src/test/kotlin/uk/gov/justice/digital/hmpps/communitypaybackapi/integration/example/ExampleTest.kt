package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.example

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.example.Example
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventListener
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class ExampleTest : IntegrationTestBase() {

  @Autowired
  lateinit var domainEventListener: DomainEventListener

  @Nested
  @DisplayName("GET /example")
  inner class ExampleEndpoint {
    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example")
        .headers(
          setAuthorisation(
            username = "INTEGRATION_TEST",
            roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("apiName").isEqualTo("hmpps-community-payback-api")
    }
  }

  @Nested
  @DisplayName("POST /example")
  inner class CreateExampleEndpoint {
    @Test
    fun `should create and return example, raising a domain event`() {
      webTestClient.post()
        .uri("/example")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example("test-api"))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("apiName").isEqualTo("test-api")

      val domainEvent = domainEventListener.blockForDomainEventOfType("community-payback.test")

      assertThat(domainEvent.description).isEqualTo("A test domain event to prove integration")
      assertThat(domainEvent.occurredAt).isCloseTo(
        OffsetDateTime.now(),
        within(1, ChronoUnit.SECONDS),
      )
    }
  }

  @Nested
  @DisplayName("PUT /example/{id}")
  inner class UpdateExampleEndpoint {
    @Test
    fun `should update and return example`() {
      webTestClient.put()
        .uri("/example/123")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example("test-api"))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("apiName").isEqualTo("test-api-updated")
    }
  }

  @Nested
  @DisplayName("DELETE /example/{id}")
  inner class DeleteExampleEndpoint {
    @Test
    fun `should delete example`() {
      webTestClient.delete()
        .uri("/example/123")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  @DisplayName("GET /example/error")
  inner class ErrorEndpoint {
    @Test
    fun `should return a 500 error`() {
      webTestClient.get()
        .uri("/example/error")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }
}

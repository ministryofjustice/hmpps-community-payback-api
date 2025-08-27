package uk.gov.justice.digital.hmpps.communitypaybackapi.integration.example

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.example.Example
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.IntegrationTestBase

class ExampleTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /example")
  inner class ExampleEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example")
        .headers(setAuthorisation(roles = listOf("ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI")))
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
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/example")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example("test-api"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/example")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example("test-api"))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/example")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example("test-api"))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should create and return example`() {
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
    }
  }

  @Nested
  @DisplayName("PUT /example/{id}")
  inner class UpdateExampleEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/example/123")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example(apiName = "test-api"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/example/123")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example(apiName = "test-api"))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/example/123")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Example(apiName = "test-api"))
        .exchange()
        .expectStatus()
        .isForbidden
    }

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
    fun `should return unauthorized if no token`() {
      webTestClient.delete()
        .uri("/example/123")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.delete()
        .uri("/example/123")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.delete()
        .uri("/example/123")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

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
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/example/error")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/example/error")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/example/error")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

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

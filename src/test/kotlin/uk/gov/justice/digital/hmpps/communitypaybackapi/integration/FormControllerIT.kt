package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

class FormControllerIT : IntegrationTestBase() {

  @Test
  fun `should return unauthorized if no token`() {
    webTestClient.get()
      .uri("/forms/appointment/unknown")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if no role`() {
    webTestClient.get()
      .uri("/forms/appointment/unknown")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.get()
      .uri("/forms/appointment/unknown")
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `GET returns 404 when no data`() {
    webTestClient.get()
      .uri("/forms/appointment/unknown-id")
      .addUiAuthHeader()
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `PUT stores and GET returns the JSON`() {
    val formType = "appointment"
    val id = "abc-123"
    val json = """{"x":42,"y":"test"}"""

    // PUT store
    webTestClient.put()
      .uri("/forms/$formType/$id")
      .addUiAuthHeader()
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(json)
      .exchange()
      .expectStatus().isOk

    // GET retrieve
    webTestClient.get()
      .uri("/forms/$formType/$id")
      .addUiAuthHeader()
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
      .expectBody<String>()
      .isEqualTo(json)
  }
}

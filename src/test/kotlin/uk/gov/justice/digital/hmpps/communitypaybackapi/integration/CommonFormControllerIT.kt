package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository

class CommonFormControllerIT : IntegrationTestBase() {

  private companion object {
    const val FORM_TYPE: String = "appointment"
    const val FORM_ID: String = "abc-123"
  }

  @Autowired
  lateinit var formCacheEntityRepository: FormCacheEntityRepository

  @BeforeEach
  fun beforeEach() {
    formCacheEntityRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /common/forms/{formType}/{id}")
  inner class GetFormData {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `GET returns 404 when no entry`() {
      webTestClient.get()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .addAdminUiAuthHeader()
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `GET returns the JSON for existing entry`() {
      formCacheEntityRepository.save(
        FormCacheEntity(
          formId = FORM_ID,
          formType = FORM_TYPE,
          formData = """{"x":42,"y":"test"}""",
        ),
      )

      webTestClient.get()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus().isOk
        .expectBody<String>()
        .isEqualTo("""{"x":42,"y":"test"}""")
    }
  }

  @Nested
  @DisplayName("PUT /common/forms/{formType}/{id}")
  inner class PutFormData {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Adds a new value`() {
      val json = """{"x":42,"y":"test"}"""

      webTestClient.put()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .addAdminUiAuthHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(json)
        .exchange()
        .expectStatus().isOk

      val entry = formCacheEntityRepository.findAll().first()

      assertThat(entry.formType).isEqualTo(FORM_TYPE)
      assertThat(entry.formId).isEqualTo(FORM_ID)
      assertThat(entry.formData).isEqualTo("""{"x":42,"y":"test"}""")
    }

    @Test
    fun `Updates an existing value`() {
      formCacheEntityRepository.save(
        FormCacheEntity(
          formId = FORM_ID,
          formType = FORM_TYPE,
          formData = "{}",
        ),
      )

      val json = """{"x":42,"y":"test"}"""

      webTestClient.put()
        .uri("/common/forms/$FORM_TYPE/$FORM_ID")
        .addAdminUiAuthHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(json)
        .exchange()
        .expectStatus().isOk

      val entry = formCacheEntityRepository.findAll().first()

      assertThat(entry.formType).isEqualTo(FORM_TYPE)
      assertThat(entry.formId).isEqualTo(FORM_ID)
      assertThat(entry.formData).isEqualTo("""{"x":42,"y":"test"}""")
    }
  }
}

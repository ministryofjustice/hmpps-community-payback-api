package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.Test

class NotFoundIT : IntegrationTestBase() {

  @Test
  fun `Resources that aren't found should return 404 - test of the exception handler`() {
    webTestClient.get().uri("/some-url-not-found")
      .headers(setAuthorisation())
      .addAdminUiAuthHeader()
      .exchange()
      .expectStatus().isNotFound
  }
}

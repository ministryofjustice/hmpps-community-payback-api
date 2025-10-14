package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.AllRoshRisk
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ArnsMockServer
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class OffenderIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /offender/{crn}/riskSummary")
  inner class GetOffenderRisk {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/offender/CRN123/riskSummary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/offender/CRN123/riskSummary")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/offender/CRN123/riskSummary")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if offender risk can't be found`() {
      ArnsMockServer.roshNotFound("CRN123")

      val response = webTestClient.get()
        .uri("/offender/CRN123/riskSummary")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: CRN not found for ID 'CRN123'")
    }

    @Test
    fun `Should return offender risk summary if found`() {
      ArnsMockServer.rosh(
        crn = "CRN123",
        allRoshRisk = AllRoshRisk(RiskRoshSummary(OverallRiskLevel.MEDIUM)),
      )

      val response = webTestClient.get()
        .uri("/offender/CRN123/riskSummary")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<String>()

      assertThat(response).isEqualTo("MEDIUM")
    }
  }
}

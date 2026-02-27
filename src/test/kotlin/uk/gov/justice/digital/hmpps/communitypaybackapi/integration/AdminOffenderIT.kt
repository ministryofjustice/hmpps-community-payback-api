package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class AdminOffenderIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /admin/offenders/{crn}/summary")
  inner class GetOffenderSummaryEndpoint {

    private val crn = "X123456"

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/offenders/$crn/summary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/offenders/$crn/summary")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/offenders/$crn/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with offender summary`() {
      val ndCaseDetail = NDCaseDetail.valid()

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(crn, listOf(ndCaseDetail))

      val result = webTestClient.get()
        .uri("/admin/offenders/$crn/summary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CaseDetailsSummaryDto>()

      assertThat(result.unpaidWorkDetails.size).isEqualTo(1)
      val caseSummaryDetail = result.unpaidWorkDetails[0] as UnpaidWorkDetailsDto
      assertThat(caseSummaryDetail.eventNumber).isEqualTo(ndCaseDetail.eventNumber)
      assertThat(caseSummaryDetail.requiredMinutes).isEqualTo(ndCaseDetail.requiredMinutes)
      assertThat(caseSummaryDetail.completedEteMinutes).isEqualTo(ndCaseDetail.completedEteMinutes)
      assertThat(caseSummaryDetail.adjustments).isEqualTo(ndCaseDetail.adjustments)
    }

    @Test
    fun `should return 404 when offender not found`() {
      val nonExistentCrn = "NONEXISTENT"

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummaryNotFound(crn = nonExistentCrn)

      webTestClient.get()
        .uri("/admin/offenders/$nonExistentCrn/summary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}

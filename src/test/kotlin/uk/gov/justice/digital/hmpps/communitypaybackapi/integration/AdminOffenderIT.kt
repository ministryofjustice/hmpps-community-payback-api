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

  companion object {
    const val CRN = "X123456"
    const val DELIUS_EVENT_NUMBER = 92L
  }

  @Nested
  @DisplayName("GET /admin/offenders/{crn}/summary")
  inner class GetOffenderSummaryEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/summary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/summary")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with offender summary`() {
      val ndCaseDetail = NDCaseDetail.valid()

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(CRN, listOf(ndCaseDetail))

      val result = webTestClient.get()
        .uri("/admin/offenders/$CRN/summary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CaseDetailsSummaryDto>()

      assertThat(result.unpaidWorkDetails.size).isEqualTo(1)
      val caseSummaryDetail = result.unpaidWorkDetails[0]
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

  @Nested
  @DisplayName("GET /admin/offenders/{crn}/unpaid-work-details/{deliusEventNumber}")
  inner class GetUnpaidWorkDetails {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with unpaid work details`() {
      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = CRN,
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(eventNumber = DELIUS_EVENT_NUMBER),
        ),
      )

      val result = webTestClient.get()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<UnpaidWorkDetailsDto>()

      assertThat(result.eventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
    }
  }
}

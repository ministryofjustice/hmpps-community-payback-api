package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AllRoshRisk
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CommonCaseDetailsSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ArnsMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CommonOffenderIT : IntegrationTestBase() {

  companion object {
    const val CRN = "X123456"
  }

  @Nested
  @DisplayName("GET /common/offenders/{crn}/summary")
  inner class GetOffenderSummaryEndpoint {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/offenders/$CRN/summary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/offenders/$CRN/summary")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/offenders/$CRN/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_COMMUNITY_PAYBACK__COMMUNITY_PAYBACK_UI", "ROLE_COMMUNITY_PAYBACK__SUPERVISOR"])
    fun `should return OK with offender summary`(role: String) {
      val ndCaseDetail = NDUpwDetails.valid().copy(requiredMinutes = 1000, completedMinutes = 400, adjustments = -60)
      val ndCaseSummary = NDCaseSummary.Companion.valid()
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, ndCaseSummary, listOf(ndCaseDetail), "AUTH_ADM")

      val result = webTestClient.get()
        .uri("/common/offenders/$CRN/summary")
        .headers(setAuthorisation(username = "AUTH_ADM", roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CommonCaseDetailsSummaryDto>()

      assertThat(result.unpaidWorkDetails.size).isEqualTo(1)
      val caseSummaryDetail = result.unpaidWorkDetails[0]
      assertThat(caseSummaryDetail.eventNumber).isEqualTo(ndCaseDetail.eventNumber)
      assertThat(caseSummaryDetail.requiredMinutes).isEqualTo(ndCaseDetail.requiredMinutes)
      assertThat(caseSummaryDetail.completedEteMinutes).isEqualTo(ndCaseDetail.completedEteMinutes)
      assertThat(caseSummaryDetail.totalAdjustmentMinutes).isEqualTo(-60)
      assertThat(caseSummaryDetail.attendedAppointmentsMinutes).isEqualTo(400)
      assertThat(caseSummaryDetail.completedMinutes).isEqualTo(460)
      assertThat(caseSummaryDetail.remainingMinutes).isEqualTo(540)
    }

    @Test
    fun `admin summary retains original minute fields and values`() {
      val details = NDUpwDetails.valid().copy(requiredMinutes = 1000, completedMinutes = 400, adjustments = -60)
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, NDCaseSummary.valid(), listOf(details), "AUTH_ADM")

      webTestClient.get()
        .uri("/admin/offenders/$CRN/summary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.unpaidWorkDetails[0].completedMinutes").isEqualTo(400)
        .jsonPath("$.unpaidWorkDetails[0].adjustments").isEqualTo(-60)
        .jsonPath("$.unpaidWorkDetails[0].attendedAppointmentsMinutes").doesNotExist()
        .jsonPath("$.unpaidWorkDetails[0].totalAdjustmentMinutes").doesNotExist()
        .jsonPath("$.unpaidWorkDetails[0].remainingMinutes").doesNotExist()
    }

    @Test
    fun `should return 404 when offender not found`() {
      val nonExistentCrn = "NONEXISTENT"

      CommunityPaybackAndDeliusMockServer.setGetUpwDetailsSummary404Response(crn = nonExistentCrn)

      webTestClient.get()
        .uri("/common/offenders/$nonExistentCrn/summary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /common/offenders/{crn}/riskSummary")
  inner class GetOffenderRisk {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/common/offenders/CRN123/riskSummary")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/common/offenders/CRN123/riskSummary")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/common/offenders/CRN123/riskSummary")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if offender risk can't be found`() {
      ArnsMockServer.roshNotFound("CRN123")

      val response = webTestClient.get()
        .uri("/common/offenders/CRN123/riskSummary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Risk Summary not found for ID 'CRN123'")
    }

    @Test
    fun `Should return offender risk summary if found`() {
      ArnsMockServer.rosh(
        crn = "CRN123",
        allRoshRisk = AllRoshRisk(RiskRoshSummary(OverallRiskLevel.MEDIUM)),
      )

      val response = webTestClient.get()
        .uri("/common/offenders/CRN123/riskSummary")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<String>()

      assertThat(response).isEqualTo("MEDIUM")
    }
  }
}

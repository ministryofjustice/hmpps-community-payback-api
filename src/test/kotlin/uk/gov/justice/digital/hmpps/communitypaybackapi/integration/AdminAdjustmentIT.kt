package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class AdminAdjustmentIT : IntegrationTestBase() {

  companion object {
    const val CRN = "X123456"
    const val DELIUS_EVENT_NUMBER = 92
  }

  @Nested
  @DisplayName("POST /admin/offenders/{crn}/unpaid-work-details/{deliusEventNumber}/adjustments")
  inner class PostAdjustment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should create an adjustment upstream`() {
      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = CRN,
        case = NDCaseSummary.valid(),
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(eventNumber = DELIUS_EVENT_NUMBER),
        ),
        username = "theusername",
      )

      CommunityPaybackAndDeliusMockServer.postAdjustment(username = "theusername")

      webTestClient.post()
        .uri("/admin/offenders/$CRN/unpaid-work-details/$DELIUS_EVENT_NUMBER/adjustments")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateAdjustmentDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isOk

      CommunityPaybackAndDeliusMockServer.postAdjustmentVerify(username = "theusername")
    }
  }
}

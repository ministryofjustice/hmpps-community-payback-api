package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.AdminOffenderIT.Companion.CRN
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.AdminOffenderIT.Companion.DELIUS_EVENT_NUMBER
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class AdminUpwDetailsIT : IntegrationTestBase() {

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
      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(
        crn = CRN,
        case = NDCaseSummary.Companion.valid(),
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(eventNumber = DELIUS_EVENT_NUMBER),
        ),
        username = "AUTH_ADM",
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

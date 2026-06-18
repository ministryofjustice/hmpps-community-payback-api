package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementProgress
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementSubType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUnpaidWorkRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeMetadata
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.IncentiveSchemeStatus
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class IncentiveSchemeIT : IntegrationTestBase() {
  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  private val caseSummary = NDCaseSummary.valid()
  private val unpaidWorkDetails by lazy { listOf(validUpwDetails()) }

  private fun validUpwDetails(
    eventNumber: Int? = null,
    eventOutcomeCode: String? = null,
    requirementCode: String? = null,
  ): NDUpwDetails = NDUpwDetails.valid(ctx)
    .copy(eventNumber = eventNumber ?: EVENT_NUMBER)
    .let { if (eventOutcomeCode != null) it.copy(eventOutcomeCode = eventOutcomeCode) else it }
    .let { if (requirementCode != null) it.copy(unpaidWorkRequirements = listOf(NDRequirementSubType(subType = NDCodeDescription.valid().copy(code = requirementCode)))) else it }

  companion object {
    private const val CRN = "X123456"
    private const val EVENT_NUMBER = 1
  }

  @Nested
  @DisplayName("GET /admin/incentive-scheme/details/{crn}/{deliusEventNumber}")
  inner class GetIncentiveSchemeDetails {
    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with correct response when ineligible`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val unpaidWorkDetails = listOf(validUpwDetails(eventOutcomeCode = "SDO"))

      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, caseSummary, unpaidWorkDetails)
      CommunityPaybackAndDeliusMockServer.setupGetUnpaidWorkRequirementResponse(CRN, EVENT_NUMBER, requirement)
      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        username = "theusername",
        toDate = LocalDate.now(),
        pageNumber = 0,
        sortString = null,
        pageSize = Int.MAX_VALUE,
        appointments = emptyList(),
      )
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())

      val result = webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<IncentiveSchemeDetails>()

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ZERO)
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ZERO)
      assertThat(result.projectedDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.currentDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.INELIGIBLE)
    }

    @Test
    fun `should return OK with correct response when disqualified`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val qualifyingContactOutcome = contactOutcomeEntityRepository.findAll().first { !it.enforceable }
      val disqualifyingContactOutcome = contactOutcomeEntityRepository.findAll().first { it.enforceable }

      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, caseSummary, unpaidWorkDetails)
      CommunityPaybackAndDeliusMockServer.setupGetUnpaidWorkRequirementResponse(CRN, EVENT_NUMBER, requirement)
      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        username = "theusername",
        toDate = LocalDate.now(),
        pageNumber = 0,
        sortString = null,
        pageSize = Int.MAX_VALUE,
        appointments = listOf(
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(qualifyingContactOutcome.code, qualifyingContactOutcome.name),
            date = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(disqualifyingContactOutcome.code, disqualifyingContactOutcome.name),
            date = LocalDate.of(2026, 6, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())

      val result = webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<IncentiveSchemeDetails>()

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(8))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(16))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.currentDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.DISQUALIFIED)
    }

    @Test
    fun `should return OK with correct response when eligible`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val contactOutcome = contactOutcomeEntityRepository.findAll().first { !it.enforceable }

      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, caseSummary, unpaidWorkDetails)
      CommunityPaybackAndDeliusMockServer.setupGetUnpaidWorkRequirementResponse(CRN, EVENT_NUMBER, requirement)
      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        username = "theusername",
        toDate = LocalDate.now(),
        pageNumber = 0,
        sortString = null,
        pageSize = Int.MAX_VALUE,
        appointments = listOf(
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 15),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())

      val result = webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<IncentiveSchemeDetails>()

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(24))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(24))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ofHours(25))
      assertThat(result.currentDiscount).isEqualTo(Duration.ZERO)
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.ELIGIBLE)
    }

    @Test
    fun `should return OK with correct response when qualifying`() {
      val requirementProgress = NDRequirementProgress.valid().copy(requiredMinutes = 6000)
      val requirement = NDUnpaidWorkRequirement.valid().copy(requirementProgress = requirementProgress)

      val contactOutcome = contactOutcomeEntityRepository.findAll().first { !it.enforceable }

      CommunityPaybackAndDeliusMockServer.setupGetUpwDetailsSummaryResponse(CRN, caseSummary, unpaidWorkDetails)
      CommunityPaybackAndDeliusMockServer.setupGetUnpaidWorkRequirementResponse(CRN, EVENT_NUMBER, requirement)
      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        crn = CRN,
        eventNumber = EVENT_NUMBER,
        username = "theusername",
        toDate = LocalDate.now(),
        pageNumber = 0,
        sortString = null,
        pageSize = Int.MAX_VALUE,
        appointments = listOf(
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 1),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 8),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 15),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
          NDAppointmentSummary.valid().copy(
            outcome = NDContactOutcome(contactOutcome.code, contactOutcome.name),
            date = LocalDate.of(2026, 6, 22),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 0),
            minutesCredited = 480,
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.setupGetAdjustmentsResponse(CRN, EVENT_NUMBER, emptyList())

      val result = webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<IncentiveSchemeDetails>()

      assertThat(result.totalRequirement).isEqualTo(Duration.ofHours(100))
      assertThat(result.qualifyingThreshold).isEqualTo(Duration.ofHours(25))
      assertThat(result.qualifyingTimeWorked).isEqualTo(Duration.ofHours(32))
      assertThat(result.totalTimeWorked).isEqualTo(Duration.ofHours(32))
      assertThat(result.projectedDiscount).isEqualTo(Duration.ofHours(25))
      assertThat(result.currentDiscount).isEqualTo(Duration.ofMinutes(3 * 60 + 30))
      assertThat(result.status).isEqualTo(IncentiveSchemeStatus.QUALIFYING)
    }
  }

  @Nested
  @DisplayName("GET /admin/incentive-scheme/metadata")
  inner class GetIncentiveSchemeMetadata {
    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/incentive-scheme/details/$CRN/$EVENT_NUMBER")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return OK with correct response`() {
      val result = webTestClient.get()
        .uri("/admin/incentive-scheme/metadata")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<IncentiveSchemeMetadata>()

      assertThat(result.qualifyingThresholdPercentage).isEqualTo("25%")
      assertThat(result.discountPercentage).isEqualTo("50%")
      assertThat(result.maximumDiscountPercentage).isEqualTo("25%")
    }
  }
}

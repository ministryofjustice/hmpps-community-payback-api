package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCodeDescription
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDRequirementSubType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentiveSchemeEligibilityScope
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.IncentiveSchemeEligibilityScopeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEligibilityService
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class IncentiveSchemeEligibilityServiceTest {
  @MockK
  lateinit var incentiveSchemeEligibilityScopeEntityRepository: IncentiveSchemeEligibilityScopeEntityRepository

  @MockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @InjectMockKs
  lateinit var service: IncentiveSchemeEligibilityService

  @Test
  fun `isEligible returns false if no case summary could be found for CRN`() {
    val crn = String.random(6)
    val deliusEventNumber = Int.random(1, 10)

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(any(), any()) } throws WebClientResponseException.create(
      404,
      "Not Found",
      HttpHeaders(),
      ByteArray(0),
      null,
    )

    val result = service.isEligible(crn, deliusEventNumber)

    assertThat(result).isFalse
  }

  @Test
  fun `isEligible returns false if no UPW details could be found on the summary with the correct Delius event number`() {
    val crn = String.random(6)
    val deliusEventNumber = Int.random(1, 10)

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(any(), any()) } returns NDCaseDetailsSummary.valid().copy(
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(eventNumber = 999),
      ),
    )

    val result = service.isEligible(crn, deliusEventNumber)

    assertThat(result).isFalse
  }

  @Test
  fun `isEligible returns false if the sentence order is not in scope`() {
    val crn = String.random(6)
    val deliusEventNumber = Int.random(1, 10)

    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(IncentiveSchemeEligibilityScope.OUTCOME, any()) } returns false

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(any(), any()) } returns NDCaseDetailsSummary.valid().copy(
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(eventNumber = deliusEventNumber),
      ),
    )

    val result = service.isEligible(crn, deliusEventNumber)

    assertThat(result).isFalse
  }

  @Test
  fun `isEligible returns false if the requirement subtype is not in scope`() {
    val crn = String.random(6)
    val deliusEventNumber = Int.random(1, 10)

    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(IncentiveSchemeEligibilityScope.OUTCOME, any()) } returns true
    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(IncentiveSchemeEligibilityScope.REQUIREMENT_SUBTYPE, any()) } returns false

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(any(), any()) } returns NDCaseDetailsSummary.valid().copy(
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(
          eventNumber = deliusEventNumber,
          unpaidWorkRequirements = listOf(
            NDRequirementSubType.valid(),
          ),
        ),
      ),
    )

    val result = service.isEligible(crn, deliusEventNumber)

    assertThat(result).isFalse
  }

  @ParameterizedTest
  @MethodSource("validOutcomeAndRequirementCodes")
  fun `isEligible returns true if the sentence order is in scope and any requirement subtype is in scope`(
    eventOutcomeCode: String,
    upwRequirementCode: String,
  ) {
    val crn = String.random(6)
    val deliusEventNumber = Int.random(1, 10)

    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(any(), any()) } returns false
    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(IncentiveSchemeEligibilityScope.OUTCOME, eventOutcomeCode) } returns true
    every { incentiveSchemeEligibilityScopeEntityRepository.isEligible(IncentiveSchemeEligibilityScope.REQUIREMENT_SUBTYPE, upwRequirementCode) } returns true

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(any(), any()) } returns NDCaseDetailsSummary.valid().copy(
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(
          eventNumber = deliusEventNumber,
          eventOutcomeCode = eventOutcomeCode,
          unpaidWorkRequirements = listOf(
            NDRequirementSubType.valid(),
            NDRequirementSubType.valid().copy(subType = NDCodeDescription.valid().copy(code = upwRequirementCode)),
          ),
        ),
      ),
    )

    val result = service.isEligible(crn, deliusEventNumber)

    assertThat(result).isTrue
  }

  companion object {
    @JvmStatic
    fun validOutcomeAndRequirementCodes(): Stream<Arguments> {
      val eventOutcomeCodes = listOf("340", "341")
      val requirementCodes = listOf("W01", "W06", "W07")

      return eventOutcomeCodes.flatMap { eventOutcomeCode ->
        requirementCodes.map { requirementCode -> Arguments.of(eventOutcomeCode, requirementCode) }
      }.stream()
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AllRoshRisk
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class OffenderServiceTest {

  companion object {
    const val CRN1 = "CRN1"
    const val CRN2 = "CRN2"
    const val CRN3 = "CRN3"
    const val CRN4 = "CRN4"
    const val USERNAME = "the-username"
  }

  @MockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK
  lateinit var arnsClient: ArnsClient

  @MockK
  lateinit var contextService: ContextService

  @InjectMockKs
  private lateinit var service: OffenderService

  @Nested
  inner class ToOffenderInfo {

    @BeforeEach
    fun setupMocks() {
      every { contextService.getUserName() } returns USERNAME
    }

    @Test
    fun `No case summaries provided, return empty list`() {
      assertThat(
        service.toOffenderInfos(emptyList()),
      ).isEmpty()
    }

    @Test
    fun `No limited access offenders in provided case summaries, don't call user access endpoint`() {
      val crn1CaseSummary = CaseSummary.valid().copy(crn = CRN1)
      val crn2CaseSummary = CaseSummary.valid().copy(crn = CRN2)

      val result = service.toOffenderInfos(listOf(crn1CaseSummary, crn2CaseSummary))

      assertThat(result).hasSize(2)

      val result1 = result[0]
      assertThat(result1).isInstanceOf(OffenderInfoResult.Full::class.java)
      result1 as OffenderInfoResult.Full
      assertThat(result1.crn).isEqualTo(CRN1)
      assertThat(result1.summary).isEqualTo(crn1CaseSummary)

      val result2 = result[1]
      assertThat(result2).isInstanceOf(OffenderInfoResult.Full::class.java)
      result2 as OffenderInfoResult.Full
      assertThat(result2.crn).isEqualTo(CRN2)
      assertThat(result2.summary).isEqualTo(crn2CaseSummary)

      verify(exactly = 0) { communityPaybackAndDeliusClient.getUsersAccess(USERNAME, any()) }
    }

    @Test
    fun `Offender with restriction that doesn't apply to the user, return full offender info`() {
      val crn1CaseSummary = CaseSummary.valid().copy(
        crn = CRN1,
        currentExclusion = false,
        currentRestriction = true,
      )

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN1))
      } returns UserAccess(listOf(CaseAccess(CRN1, userExcluded = false, userRestricted = false)))

      val result = service.toOffenderInfos(listOf(crn1CaseSummary))

      assertThat(result).hasSize(1)

      val result1 = result[0]
      assertThat(result1).isInstanceOf(OffenderInfoResult.Full::class.java)
      result1 as OffenderInfoResult.Full
      assertThat(result1.crn).isEqualTo(CRN1)
      assertThat(result1.summary).isEqualTo(crn1CaseSummary)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
    )
    fun `Offender with limited access that applies to the user, return limited offender`(
      isExcluded: Boolean,
      isRestricted: Boolean,
    ) {
      val crn1CaseSummary = CaseSummary.valid().copy(
        crn = CRN1,
        currentExclusion = isExcluded,
        currentRestriction = isRestricted,
      )

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN1))
      } returns UserAccess(listOf(CaseAccess(CRN1, isExcluded, isRestricted)))

      val result = service.toOffenderInfos(listOf(crn1CaseSummary))

      assertThat(result).hasSize(1)

      val result1 = result[0]
      assertThat(result1).isInstanceOf(OffenderInfoResult.Limited::class.java)
      assertThat(result1.crn).isEqualTo(CRN1)
    }

    /**
     * CRN1 - not limited
     * CRN2 - restricted
     * CRN3 - excluded
     */
    @Test
    fun `All variations in one result`() {
      val crn1CaseSummary = CaseSummary.valid().copy(
        crn = CRN1,
      )

      val crn2CaseSummaryRestriction = CaseSummary.valid().copy(
        crn = CRN2,
        currentExclusion = false,
        currentRestriction = true,
      )

      val crn3CaseSummaryExclusion = CaseSummary.valid().copy(
        crn = CRN3,
        currentExclusion = true,
        currentRestriction = false,
      )

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN2, CRN3))
      } returns UserAccess(
        listOf(
          CaseAccess(CRN2, userExcluded = false, userRestricted = true),
          CaseAccess(CRN3, userExcluded = true, userRestricted = false),
        ),
      )

      val result = service.toOffenderInfos(listOf(crn1CaseSummary, crn2CaseSummaryRestriction, crn3CaseSummaryExclusion))

      assertThat(result).hasSize(3)

      assertThat(result[0].crn).isEqualTo(CRN1)
      assertThat(result[0]).isInstanceOf(OffenderInfoResult.Full::class.java)

      assertThat(result[1].crn).isEqualTo(CRN2)
      assertThat(result[1]).isInstanceOf(OffenderInfoResult.Limited::class.java)

      assertThat(result[2].crn).isEqualTo(CRN3)
      assertThat(result[2]).isInstanceOf(OffenderInfoResult.Limited::class.java)
    }
  }

  @Nested
  inner class GetRiskSummary {

    @Test
    fun `Entry exists, return value`() {
      every { arnsClient.rosh("CRN123") } returns AllRoshRisk(RiskRoshSummary(OverallRiskLevel.HIGH))

      val result = service.getRiskSummary("CRN123")

      assertThat(result).isEqualTo("HIGH")
    }

    @Test
    fun `Entry doesnt exist, return 404`() {
      every { arnsClient.rosh("CRN123") } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getRiskSummary("CRN123")
      }.isInstanceOf(NotFoundException::class.java).hasMessage("CRN not found for ID 'CRN123'")
    }
  }
}

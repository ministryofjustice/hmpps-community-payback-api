package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.service

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
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.AllRoshRisk
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.UserAccess
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderInfoResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.service.OffenderService
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
  inner class GetOffenderInfo {

    @BeforeEach
    fun setupMocks() {
      every { contextService.getUserName() } returns USERNAME
    }

    @Test
    fun `No CRNs, returns empty list`() {
      assertThat(
        service.getOffenderInfo(emptySet()),
      ).isEmpty()
    }

    @Test
    fun `Requested number of CRNs exceeds batch size, throws exception`() {
      assertThatThrownBy {
        service.getOffenderInfo((1..501).map { it.toString() }.toSet())
      }.hasMessage("Can only request up-to 500 CRNs. Have requested 501.")
    }

    @Test
    fun `Offender not found`() {
      every { communityPaybackAndDeliusClient.getCaseSummaries(setOf(CRN1)) } returns CaseSummaries(emptyList())

      val result = service.getOffenderInfo(setOf(CRN1))

      assertThat(result).hasSize(1)

      val result1 = result[0]
      assertThat(result1).isInstanceOf(OffenderInfoResult.NotFound::class.java)
      assertThat(result1.crn).isEqualTo(CRN1)
    }

    @Test
    fun `No limited access offenders, don't call user access endpoint`() {
      val crn1CaseSummary = CaseSummary(
        crn = CRN1,
        name = CaseName(forename = "fn1", surname = "cn1"),
      )

      val crn2CaseSummary = CaseSummary(
        crn = CRN2,
        name = CaseName(forename = "fn2", surname = "cn2"),
      )

      every {
        communityPaybackAndDeliusClient.getCaseSummaries(setOf(CRN1, CRN2))
      } returns CaseSummaries(listOf(crn1CaseSummary, crn2CaseSummary))

      val result = service.getOffenderInfo(setOf(CRN1, CRN2))

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
    fun `Offender has limited access, but user can access them`() {
      val crn1CaseSummary = CaseSummary(
        crn = CRN1,
        name = CaseName(forename = "fn1", surname = "cn1"),
        currentExclusion = false,
        currentRestriction = true,
      )

      every { communityPaybackAndDeliusClient.getCaseSummaries(setOf(CRN1)) } returns CaseSummaries(listOf(crn1CaseSummary))

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN1))
      } returns UserAccess(listOf(CaseAccess(CRN1, userExcluded = false, userRestricted = false)))

      val result = service.getOffenderInfo(setOf(CRN1))

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
    fun `Offender has limited access that apply to user`(
      isExcluded: Boolean,
      isRestricted: Boolean,
    ) {
      val crn1CaseSummary = CaseSummary(
        crn = CRN1,
        name = CaseName(forename = "fn1", surname = "cn1"),
        currentExclusion = isExcluded,
        currentRestriction = isRestricted,
      )

      every { communityPaybackAndDeliusClient.getCaseSummaries(setOf(CRN1)) } returns CaseSummaries(listOf(crn1CaseSummary))

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN1))
      } returns UserAccess(listOf(CaseAccess(CRN1, isExcluded, isRestricted)))

      val result = service.getOffenderInfo(setOf(CRN1))

      assertThat(result).hasSize(1)

      val result1 = result[0]
      assertThat(result1).isInstanceOf(OffenderInfoResult.Limited::class.java)
      assertThat(result1.crn).isEqualTo(CRN1)
    }

    /**
     * CRN1 - not found
     * CRN2 - not limited
     * CRN3 - restricted
     * CRN4 - excluded
     */
    @Test
    fun `All variations in one result`() {
      val crn2CaseSummary = CaseSummary(
        crn = CRN2,
        name = CaseName(forename = "fn2", surname = "cn2"),
      )

      val crn3CaseSummary = CaseSummary(
        crn = CRN3,
        name = CaseName(forename = "fn3", surname = "cn3"),
        currentExclusion = false,
        currentRestriction = true,
      )

      val crn4CaseSummary = CaseSummary(
        crn = CRN4,
        name = CaseName(forename = "fn4", surname = "cn4"),
        currentExclusion = true,
        currentRestriction = false,
      )

      every {
        communityPaybackAndDeliusClient.getCaseSummaries(setOf(CRN1, CRN2, CRN3, CRN4))
      } returns CaseSummaries(listOf(crn2CaseSummary, crn3CaseSummary, crn4CaseSummary))

      every {
        communityPaybackAndDeliusClient.getUsersAccess(USERNAME, setOf(CRN3, CRN4))
      } returns UserAccess(
        listOf(
          CaseAccess(CRN3, userExcluded = false, userRestricted = true),
          CaseAccess(CRN4, userExcluded = true, userRestricted = false),
        ),
      )

      val result = service.getOffenderInfo(setOf(CRN1, CRN2, CRN3, CRN4))

      assertThat(result).hasSize(4)

      assertThat(result[0].crn).isEqualTo(CRN1)
      assertThat(result[0]).isInstanceOf(OffenderInfoResult.NotFound::class.java)

      assertThat(result[1].crn).isEqualTo(CRN2)
      assertThat(result[1]).isInstanceOf(OffenderInfoResult.Full::class.java)

      assertThat(result[2].crn).isEqualTo(CRN3)
      assertThat(result[2]).isInstanceOf(OffenderInfoResult.Limited::class.java)

      assertThat(result[3].crn).isEqualTo(CRN4)
      assertThat(result[3]).isInstanceOf(OffenderInfoResult.Limited::class.java)
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.AllRoshRisk
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ArnsClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class OffenderServiceTest {

  companion object {
    const val CRN1 = "CRN1"
  }

  @MockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @MockK
  lateinit var arnsClient: ArnsClient

  @InjectMockKs
  private lateinit var service: OffenderService

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

  @Nested
  inner class GetOffenderSummaryByCrn {
    val crn = "CRN123"

    @Test
    fun `returns offender summary when found`() {
      val caseDetailsSummary = NDCaseDetailsSummary.valid().copy(
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid(),
          NDCaseDetail.valid(),
          NDCaseDetail.valid(),
          NDCaseDetail.valid(),
        ),
      )

      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(crn) } returns caseDetailsSummary

      val result = service.getOffenderSummaryByCrn(crn)

      result.unpaidWorkDetails.forEachIndexed { index, detail ->
        assertThat(detail.eventNumber).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].eventNumber)
        assertThat(detail.requiredMinutes).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].requiredMinutes)
        assertThat(detail.completedEteMinutes).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].completedEteMinutes)
        assertThat(detail.adjustments).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].adjustments)
      }
      verify(exactly = 1) { communityPaybackAndDeliusClient.getUpwDetailsSummary(crn) }
    }

    @Test
    fun `throws NotFoundException when offender not found`() {
      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(crn) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getOffenderSummaryByCrn(crn)
      }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessage("CRN not found for ID '$crn'")
    }
  }
}

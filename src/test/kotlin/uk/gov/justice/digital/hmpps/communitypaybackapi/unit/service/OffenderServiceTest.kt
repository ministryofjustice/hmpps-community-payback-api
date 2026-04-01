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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetailsSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpwDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.OverallRiskLevel
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.RiskRoshSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class OffenderServiceTest {

  companion object {
    const val CRN = "CRN1"
    const val USER_NAME = "USERNAME"
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
      every { arnsClient.rosh(CRN) } returns AllRoshRisk(RiskRoshSummary(OverallRiskLevel.HIGH))

      val result = service.getRiskSummary(CRN)

      assertThat(result).isEqualTo("HIGH")
    }

    @Test
    fun `Entry doesnt exist, return null`() {
      every { arnsClient.rosh(CRN) } throws WebClientResponseExceptionFactory.notFound()

      val result = service.getRiskSummary(CRN)

      assertThat(result).isNull()
    }
  }

  @Nested
  inner class GetOffenderSummaryByCrn {

    @Test
    fun `returns offender summary when found`() {
      val caseDetailsSummary = NDCaseDetailsSummary.valid().copy(
        unpaidWorkDetails = listOf(
          NDUpwDetails.valid(),
          NDUpwDetails.valid(),
          NDUpwDetails.valid(),
          NDUpwDetails.valid(),
        ),
      )

      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) } returns caseDetailsSummary

      val result = service.getOffenderSummaryByCrn(CRN, USER_NAME)

      result.unpaidWorkDetails.forEachIndexed { index, detail ->
        assertThat(detail.eventNumber).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].eventNumber)
        assertThat(detail.requiredMinutes).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].requiredMinutes)
        assertThat(detail.completedEteMinutes).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].completedEteMinutes)
        assertThat(detail.adjustments).isEqualTo(caseDetailsSummary.unpaidWorkDetails[index].adjustments)
      }
      verify(exactly = 1) { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) }
    }

    @Test
    fun `returns offender summary when username is null`() {
      val caseDetailsSummary = NDCaseDetailsSummary.valid()

      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, null) } returns caseDetailsSummary

      val result = service.getOffenderSummaryByCrn(CRN, null)

      assertThat(result.unpaidWorkDetails).hasSize(caseDetailsSummary.unpaidWorkDetails.size)
      verify(exactly = 1) { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, null) }
    }

    @Test
    fun `throws NotFoundException when offender not found`() {
      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getOffenderSummaryByCrn(CRN, USER_NAME)
      }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessage("Offender Summary not found for ID '$CRN'")
    }
  }

  @Nested
  inner class GetUnpaidWorkDetails {

    @Test
    fun `return details when found`() {
      val caseDetailsSummary = NDCaseDetailsSummary.valid().copy(
        unpaidWorkDetails = listOf(
          NDUpwDetails.valid().copy(
            eventNumber = 4,
            requiredMinutes = 1L,
          ),
          NDUpwDetails.valid().copy(
            eventNumber = 5,
            requiredMinutes = 2L,
          ),
          NDUpwDetails.valid().copy(
            eventNumber = 6,
            requiredMinutes = 3L,
          ),
        ),
      )

      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) } returns caseDetailsSummary

      val result = service.getUnpaidWorkDetails(CRN, 5, USER_NAME)

      assertThat(result.eventNumber).isEqualTo(5)
      assertThat(result.requiredMinutes).isEqualTo(2L)
    }

    @Test
    fun `return details when username is not passed`() {
      val caseDetailsSummary = NDCaseDetailsSummary.valid().copy(
        unpaidWorkDetails = listOf(
          NDUpwDetails.valid().copy(
            eventNumber = 5,
          ),
        ),
      )

      every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, null) } returns caseDetailsSummary

      val result = service.getUnpaidWorkDetails(CRN, 5)

      assertThat(result.eventNumber).isEqualTo(5)
      verify(exactly = 1) { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, null) }
    }
  }

  @Test
  fun `throws NotFoundException when offender not found`() {
    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) } throws WebClientResponseExceptionFactory.notFound()

    assertThatThrownBy {
      service.getUnpaidWorkDetails(CRN, 1, USER_NAME)
    }
      .isInstanceOf(NotFoundException::class.java)
      .hasMessage("Offender Summary not found for ID '$CRN'")
  }

  @Test
  fun `throws NotFoundException when unpaid work details not found`() {
    val caseDetailsSummary = NDCaseDetailsSummary.valid().copy(
      unpaidWorkDetails = listOf(
        NDUpwDetails.valid().copy(
          eventNumber = 4,
          requiredMinutes = 1L,
        ),
      ),
    )

    every { communityPaybackAndDeliusClient.getUpwDetailsSummary(CRN, USER_NAME) } returns caseDetailsSummary

    assertThatThrownBy {
      service.getUnpaidWorkDetails(CRN, 5, USER_NAME)
    }
      .isInstanceOf(NotFoundException::class.java)
      .hasMessage("Unpaid Work Details not found for ID 'CRN CRN1, Event Number 5'")
  }
}

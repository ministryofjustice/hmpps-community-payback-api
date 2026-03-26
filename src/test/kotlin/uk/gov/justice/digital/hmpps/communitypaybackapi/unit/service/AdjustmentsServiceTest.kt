package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentsService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AdjustmentsServiceTest {

  @RelaxedMockK
  private lateinit var offenderService: OffenderService

  @RelaxedMockK
  private lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  @RelaxedMockK
  private lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @InjectMockKs
  private lateinit var service: AdjustmentsService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    const val USERNAME = "username"
    val REASON_ID: UUID = UUID.fromString("74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1")
  }

  @Nested
  inner class CreateAdjustment {

    @Test
    fun `If adjustment reason not found return bad request exception`() {
      every { adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID) } returns null

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = REASON_ID,
      )

      assertThatThrownBy {
        service.createAdjustment(
          crn = CRN,
          deliusEventNumber = EVENT_NUMBER,
          createAdjustment = request,
          username = USERNAME,
        )
      }.hasMessage("Adjustment Reason not found for ID '74f0f62b-bbd4-49a4-9af8-1ce6cd94e3e1'")
    }

    @Test
    fun `If minutes more than allowed for adjustment reason return bad request exception`() {
      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns AdjustmentReasonEntity.valid().copy(
        name = "The reason name",
        maxMinutesAllowed = 50,
      )

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = REASON_ID,
        minutes = 51,
      )

      assertThatThrownBy {
        service.createAdjustment(
          crn = CRN,
          deliusEventNumber = EVENT_NUMBER,
          createAdjustment = request,
          username = USERNAME,
        )
      }.hasMessage("Requested adjustment minutes 51 exceeds the maximum of 50 minutes allowed for adjustments with reason 'The reason name'")
    }

    @Test
    fun success() {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)

      every {
        adjustmentReasonEntityRepository.findByIdOrNull(REASON_ID)
      } returns reason

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = REASON_ID,
        minutes = 50,
      )

      service.createAdjustment(
        crn = CRN,
        deliusEventNumber = EVENT_NUMBER,
        createAdjustment = request,
        username = USERNAME,
      )

      verify {
        communityPaybackAndDeliusClient.postAdjustments(
          username = "username",
          adjustmentRequests = listOf(
            request.toNDAdjustmentRequest(
              crn = CRN,
              deliusEventNumber = EVENT_NUMBER.toInt(),
              reason = reason,
            ),
          ),
        )
      }
    }
  }
}

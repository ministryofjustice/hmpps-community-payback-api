package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory.CreateAdjustmentEventDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Clock
import java.time.OffsetDateTime

@Service
class AdjustmentService(
  private val adjustmentEventService: AdjustmentEventService,
  private val adjustmentValidationService: AdjustmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val clock: Clock,
) {

  @Transactional
  fun createAdjustment(
    upwDetailsId: UnpaidWorkDetailsIdDto,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ) {
    val validatedAdjustment = adjustmentValidationService.validateCreate(createAdjustment, upwDetailsId, username)

    val (crn, deliusEventNumber) = upwDetailsId

    val deliusAdjustmentId = communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = validatedAdjustment.reason,
        ),
      ),
    ).single().id

    adjustmentEventService.publishCreateEventOnTransactionCommit(
      CreateAdjustmentEventDetails(
        createAdjustmentDto = createAdjustment,
        appointment = validatedAdjustment.task.appointment,
        reason = validatedAdjustment.reason,
        deliusAdjustmentId = deliusAdjustmentId,
        trigger = AdjustmentEventTrigger(
          triggeredAt = OffsetDateTime.now(clock),
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = validatedAdjustment.task.id.toString(),
        ),
      ),
    )
  }
}

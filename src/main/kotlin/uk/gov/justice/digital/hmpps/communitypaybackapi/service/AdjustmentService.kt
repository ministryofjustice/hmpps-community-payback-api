package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentService(
  private val adjustmentValidationService: AdjustmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val clock: Clock,
  private val springEventPublisher: SpringEventPublisher,
  private val adjustmentIdGenerator: AdjustmentIdGenerator,
) {

  @Transactional
  fun createAdjustment(
    upwDetailsId: UnpaidWorkDetailsIdDto,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ) {
    val validatedAdjustment = adjustmentValidationService.validateCreate(createAdjustment, upwDetailsId, username)
    val adjustmentId = adjustmentIdGenerator.generateId()
    val (crn, deliusEventNumber) = upwDetailsId

    val deliusAdjustmentId = communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = validatedAdjustment.reason,
          reference = adjustmentId,
        ),
      ),
    ).single().id

    springEventPublisher.publishEvent(
      AdjustmentCreatedEvent(
        id = adjustmentId,
        createDto = createAdjustment,
        appointmentEntity = validatedAdjustment.task.appointment,
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

  @EventListener
  fun rollbackAdjustment(
    rollbackEvent: CommunityPaybackSpringEvent.NDeliusRollbackRequired,
  ) {
    val event = rollbackEvent.event
    if (event is AdjustmentCreatedEvent) {
      communityPaybackAndDeliusClient.deleteAdjustment(event.deliusAdjustmentId)
    }
  }
}

interface AdjustmentIdGenerator {
  fun generateId(): UUID
}

@Service
class DefaultAdjustmentIdGenerator : AdjustmentIdGenerator {
  override fun generateId() = AdjustmentEventEntity.generateId()
}

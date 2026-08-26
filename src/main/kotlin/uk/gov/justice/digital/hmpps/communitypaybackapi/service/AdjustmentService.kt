package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.IdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Clock
import java.time.LocalDate
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
  fun getAdjustments(crn: String, eventNumber: Int) = communityPaybackAndDeliusClient.getAdjustments(crn, eventNumber).adjustments.map { it.toDto() }

  @Transactional
  fun createAdjustment(
    upwDetailsId: UnpaidWorkDetailsIdDto,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ) {
    val validatedAdjustment = adjustmentValidationService.validateCreate(createAdjustment, upwDetailsId, username)
    val adjustmentId = adjustmentIdGenerator.generateId(createAdjustment)

    deleteOrphanedAdjustmentIfExists(adjustmentId)

    val (crn, deliusEventNumber) = upwDetailsId
    val adjustmentDate = validatedAdjustment.createAdjustment.adjustmentDate ?: LocalDate.now(clock)

    val deliusAdjustmentId = communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = validatedAdjustment.reason,
          reference = adjustmentId,
          dateOfAdjustment = adjustmentDate,
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
        adjustmentDate = adjustmentDate,
      ),
    )
  }

  @EventListener
  fun rollbackAdjustment(
    rollbackEvent: CommunityPaybackSpringEvent.NDeliusRollbackRequired,
  ) {
    val event = rollbackEvent.event
    if (event is AdjustmentCreatedEvent) {
      communityPaybackAndDeliusClient.deleteAdjustment(event.id)
    }
  }

  private fun deleteOrphanedAdjustmentIfExists(reference: UUID) {
    try {
      communityPaybackAndDeliusClient.deleteAdjustment(reference)
    } catch (e: WebClientResponseException) {
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }
  }
}

interface AdjustmentIdGenerator {
  fun generateId(createAdjustment: CreateAdjustmentDto): UUID
}

@Service
class DefaultAdjustmentIdGenerator : AdjustmentIdGenerator {
  override fun generateId(createAdjustment: CreateAdjustmentDto) = IdGenerator(CreateAdjustmentDto::class).generateId(createAdjustment)
}

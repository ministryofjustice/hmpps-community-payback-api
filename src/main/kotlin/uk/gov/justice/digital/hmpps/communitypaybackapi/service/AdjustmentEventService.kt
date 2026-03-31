package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CreateAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAdjustmentCreatedDomainEvent
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentEventService(
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
  private val adjustmentEventEntityFactory: AdjustmentEventEntityFactory,
  private val domainEventService: DomainEventService,
) {
  fun getCreatedDomainEventDetails(id: UUID) = adjustmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AdjustmentEventType.CREATE)?.toAdjustmentCreatedDomainEvent()

  fun getEvent(eventId: UUID) = adjustmentEventEntityRepository.findByIdOrNull(eventId)

  fun publishCreateEventOnTransactionCommit(details: CreateAdjustmentEvent) {
    val persistedEvent = adjustmentEventEntityRepository.save(
      adjustmentEventEntityFactory.buildAdjustmentCreated(details),
    )

    domainEventService.publishOnTransactionCommit(
      id = persistedEvent.id,
      type = when (persistedEvent.eventType) {
        AdjustmentEventType.CREATE -> DomainEventType.ADJUSTMENT_CREATED
      },
      headers = persistedEvent.appointment.toDomainEventHeaders(),
    )
  }

  @Transactional
  fun recordSchedulingRan(
    forEventId: UUID,
    schedulingId: UUID,
  ) = adjustmentEventEntityRepository.setSchedulingRanAt(
    eventId = forEventId,
    schedulingId = schedulingId,
    now = OffsetDateTime.now(),
  )
}

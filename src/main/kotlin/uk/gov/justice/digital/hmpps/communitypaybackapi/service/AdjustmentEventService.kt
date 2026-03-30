package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventEntityFactory.CreateAdjustmentEventDetails
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toAdjustmentCreatedDomainEvent
import java.util.UUID

@Service
class AdjustmentEventService(
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
  private val adjustmentEventEntityFactory: AdjustmentEventEntityFactory,
  private val domainEventService: DomainEventService,
) {
  fun getCreatedDomainEventDetails(id: UUID) = adjustmentEventEntityRepository.findByIdOrNullForDomainEventDetails(id, AdjustmentEventType.CREATE)?.toAdjustmentCreatedDomainEvent()

  fun publishCreateEventOnTransactionCommit(details: CreateAdjustmentEventDetails) {
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
}

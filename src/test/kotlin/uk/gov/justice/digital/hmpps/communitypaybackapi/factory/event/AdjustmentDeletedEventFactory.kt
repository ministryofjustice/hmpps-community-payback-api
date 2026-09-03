package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import java.util.UUID

fun AdjustmentDeletedEvent.Companion.valid() = AdjustmentDeletedEvent(
  id = UUID.randomUUID(),
  eventToDelete = AdjustmentEventEntity.valid(),
  trigger = AdjustmentEventTrigger.valid(),
)

fun AdjustmentDeletedEvent.Companion.valid(ctx: ApplicationContext) = AdjustmentDeletedEvent(
  id = UUID.randomUUID(),
  eventToDelete = AdjustmentEventEntity.valid(ctx),
  trigger = AdjustmentEventTrigger.valid(),
)

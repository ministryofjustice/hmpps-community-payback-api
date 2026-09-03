package uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent

import java.util.UUID

data class AdjustmentDeletedDomainEventDetailsDto(
  val id: UUID,
  val adjustmentCreatedEventId: UUID,
)

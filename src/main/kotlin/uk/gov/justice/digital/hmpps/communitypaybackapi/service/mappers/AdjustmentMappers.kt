package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AdjustmentCreatedDomainEventDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AdjustmentDeletedDomainEventDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

fun NDAdjustment.toDto(): AdjustmentDto = AdjustmentDto(
  deliusId = this.id,
  id = this.reference!!,
  date = this.date,
  amount = when (this.type) {
    NDAdjustmentType.POSITIVE -> Duration.ofMinutes(this.minutes.toLong())
    NDAdjustmentType.NEGATIVE -> Duration.ofMinutes(-this.minutes.toLong())
  },
  reason = this.reason.name,
  reasonCode = this.reason.code,
)

fun CreateAdjustmentDto.toNDAdjustmentRequest(
  crn: String,
  deliusEventNumber: Int,
  reason: AdjustmentReasonEntity,
  reference: UUID,
  dateOfAdjustment: LocalDate,
) = NDAdjustmentRequest(
  crn = crn,
  eventNumber = deliusEventNumber,
  type = when (type) {
    CreateAdjustmentTypeDto.Positive -> NDAdjustmentType.POSITIVE
    CreateAdjustmentTypeDto.Negative -> NDAdjustmentType.NEGATIVE
  },
  date = dateOfAdjustment,
  reason = reason.deliusCode,
  minutes = minutes,
  reference = reference,
)

fun AdjustmentEventEntity.toAdjustmentCreatedDomainEvent() = AdjustmentCreatedDomainEventDetailsDto()

fun AdjustmentEventEntity.toAdjustmentDeletedDomainEvent() = AdjustmentDeletedDomainEventDetailsDto(
  id = this.id,
  adjustmentCreatedEventId = this.referencedEvent!!.id,
)

fun NDAdjustmentType.toAdjustmentType(): AdjustmentType = when (this) {
  NDAdjustmentType.NEGATIVE -> AdjustmentType.NEGATIVE
  NDAdjustmentType.POSITIVE -> AdjustmentType.POSITIVE
}

fun NDAdjustment.toEntity(
  crn: String,
  eventNumber: Int,
  reason: AdjustmentReasonEntity?,
) = AdjustmentEntity(
  id = reference ?: UUID.randomUUID(),
  deliusAdjustmentId = id,
  crn = crn,
  deliusEventNumber = eventNumber,
  adjustmentType = type.toAdjustmentType(),
  adjustmentDate = date,
  minutes = minutes,
  adjustmentReason = reason,
)

fun AdjustmentEntity.updateFrom(
  adjustment: NDAdjustment,
  crn: String,
  eventNumber: Int,
  reason: AdjustmentReasonEntity?,
) = apply {
  this.crn = crn
  deliusEventNumber = eventNumber
  deliusAdjustmentId = adjustment.id
  adjustmentType = adjustment.type.toAdjustmentType()
  adjustmentDate = adjustment.date
  minutes = adjustment.minutes
  adjustmentReason = reason
}

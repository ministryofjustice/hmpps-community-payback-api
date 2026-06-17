package uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers

import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.domainevent.AdjustmentCreatedDomainEventDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
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

fun AdjustmentEventEntity.toAdjustmentCreatedDomainEvent() = this.toAdjustmentDomainEvent()

private fun AdjustmentEventEntity.toAdjustmentDomainEvent() = AdjustmentCreatedDomainEventDetailsDto()

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityUpsertRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toEntity

@Service
class AdjustmentPersistenceService(
  private val adjustmentEntityRepository: AdjustmentEntityRepository,
  private val adjustmentEntityUpsertRepository: AdjustmentEntityUpsertRepository,
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
) {
  @Transactional
  fun saveAdjustments(
    crn: String,
    eventNumber: Int,
    deliusAdjustments: List<NDAdjustment>,
    appointment: AppointmentEntity? = null,
  ): List<AdjustmentDto> {
    if (deliusAdjustments.isEmpty()) return emptyList()

    val reasons = adjustmentReasonEntityRepository
      .findByDeliusCodeIn(deliusAdjustments.map { it.reason.code }.distinct())
      .associateBy { it.deliusCode }

    val appointmentsByReference = adjustmentEventEntityRepository
      .findAllById(deliusAdjustments.mapNotNull { it.reference }.distinct())
      .filter { it.eventType == AdjustmentEventType.CREATE }
      .associate { it.id to it.appointment }

    // Creation can join an existing transaction containing new appointments and adjustment events.
    adjustmentEntityRepository.flush()

    // Consistent lock ordering avoids deadlocks when overlapping requests import several adjustments.
    val savedAdjustments = deliusAdjustments.sortedBy { it.id }.associate { adjustment ->
      val linkedAppointment = appointment ?: appointmentsByReference[adjustment.reference]
      val entity = adjustment.toEntity(crn, eventNumber, reasons[adjustment.reason.code], linkedAppointment)
      val savedId = adjustmentEntityUpsertRepository.upsert(entity)
      adjustment.id to entity.toDto().copy(id = savedId)
    }

    return deliusAdjustments.map { savedAdjustments.getValue(it.id) }
  }
}

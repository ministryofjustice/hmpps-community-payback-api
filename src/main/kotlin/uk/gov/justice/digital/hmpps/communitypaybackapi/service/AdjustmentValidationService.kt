package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository

@Suppress("ThrowsCount")
@Service
class AdjustmentValidationService(
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
  private val appointmentTaskEntityRepository: AppointmentTaskEntityRepository,
  private val offenderService: OffenderService,
) {

  fun validateCreate(
    createAdjustment: CreateAdjustmentDto,
    upwDetailsId: UnpaidWorkDetailsIdDto,
    username: String,
  ): ValidatedCreateAdjustment {
    val reason = adjustmentReasonEntityRepository.findByIdOrNull(createAdjustment.adjustmentReasonId)
      ?: throw NotFoundException("Adjustment Reason", createAdjustment.adjustmentReasonId.toString())

    val task = appointmentTaskEntityRepository.findByIdOrNull(createAdjustment.taskId) ?: throw NotFoundException("Task", createAdjustment.taskId)

    offenderService.ensureUnpaidWorkDetailsExist(upwDetailsId, username)
    val requestedMinutes = createAdjustment.minutes

    val maxMinutesAllowed = reason.maxMinutesAllowed
    if (requestedMinutes > maxMinutesAllowed) {
      badRequest("Requested adjustment minutes $requestedMinutes exceeds the maximum of $maxMinutesAllowed minutes allowed for adjustments with reason '${reason.name}'")
    }

    return ValidatedCreateAdjustment(
      createAdjustment,
      reason,
      task,
    )
  }

  data class ValidatedCreateAdjustment(
    val createAdjustment: CreateAdjustmentDto,
    val reason: AdjustmentReasonEntity,
    val task: AppointmentTaskEntity,
  )
}

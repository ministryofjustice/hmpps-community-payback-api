package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.notFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import java.time.Duration

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
      ?: notFound("Adjustment Reason", createAdjustment.adjustmentReasonId.toString())

    val task = appointmentTaskEntityRepository.findByIdOrNull(createAdjustment.taskId) ?: notFound("Task", createAdjustment.taskId)

    offenderService.ensureUnpaidWorkDetailsExist(upwDetailsId, username)
    val requestedMinutes = createAdjustment.minutes

    val maxMinutesAllowed = reason.maxMinutesAllowed
    if (requestedMinutes > maxMinutesAllowed) {
      val requestedDuration = Duration.ofMinutes(requestedMinutes.toLong())
      val maxMinutesDuration = Duration.ofMinutes(maxMinutesAllowed.toLong())
      badRequest("Requested adjustment of '${requestedDuration.formatForUser()}' exceeds the maximum allowed time '${maxMinutesDuration.formatForUser()}' for adjustment reason '${reason.name}'")
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

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import java.time.Duration
import java.time.LocalDate

@Suppress("ThrowsCount")
@Service
class AdjustmentValidationService(
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
  private val appointmentEntityRepository: AppointmentEntityRepository,
  private val offenderService: OffenderService,
) {

  fun validateCreate(
    createAdjustment: CreateAdjustmentDto,
    upwDetailsId: UnpaidWorkDetailsIdDto,
    username: String,
  ): ValidatedCreateAdjustment {
    val reason = adjustmentReasonEntityRepository.findByIdOrNull(createAdjustment.adjustmentReasonId)
      ?: badRequest("Adjustment Reason not found for ID '${createAdjustment.adjustmentReasonId}'")

    val appointment = if (reason.needsLinkToAppointment) {
      if (createAdjustment.appointmentId == null) {
        badRequest("Adjustment reason '${reason.name}' needs an appointment ID")
      }

      appointmentEntityRepository.findByIdOrNull(createAdjustment.appointmentId) ?: badRequest("Appointment not found for ID '${createAdjustment.appointmentId}'")
    } else if (!reason.needsLinkToAppointment && createAdjustment.appointmentId != null) {
      badRequest("Adjustment reason '${reason.name}' does not support linking to appointments")
    } else {
      null
    }

    val unpaidWorkDetails = offenderService.ensureUnpaidWorkDetailsExist(upwDetailsId, username)
      ?: badRequest("Unpaid Work Details not found for CRN ${upwDetailsId.crn} and event number ${upwDetailsId.deliusEventNumber}")
    val requestedMinutes = createAdjustment.minutes

    val maxMinutesAllowed = reason.maxMinutesAllowed
    if (requestedMinutes > maxMinutesAllowed) {
      val requestedDuration = Duration.ofMinutes(requestedMinutes.toLong())
      val maxMinutesDuration = Duration.ofMinutes(maxMinutesAllowed.toLong())
      badRequest("Requested adjustment of '${requestedDuration.formatForUser()}' exceeds the maximum allowed time '${maxMinutesDuration.formatForUser()}' for adjustment reason '${reason.name}'")
    }

    if (createAdjustment.adjustmentDate != null && createAdjustment.adjustmentDate.isAfter(LocalDate.now())) {
      badRequest("Adjustment date must not be in the future")
    }

    if (createAdjustment.adjustmentDate != null && createAdjustment.adjustmentDate.isBefore(unpaidWorkDetails.sentenceDate)) {
      badRequest("Adjustment date must not be before the sentence date")
    }

    validateMinutesToCredit(createAdjustment, unpaidWorkDetails)

    return ValidatedCreateAdjustment(
      createAdjustment,
      reason,
      appointment,
    )
  }

  data class ValidatedCreateAdjustment(
    val createAdjustment: CreateAdjustmentDto,
    val reason: AdjustmentReasonEntity,
    val appointment: AppointmentEntity?,
  )

  private fun validateMinutesToCredit(createAdjustment: CreateAdjustmentDto, unpaidWorkDetails: UnpaidWorkDetailsDto) {
    val adjustmentMinutes = Duration.ofMinutes(createAdjustment.minutes.toLong())
    val requiredTime = Duration.ofMinutes(unpaidWorkDetails.requiredMinutes + unpaidWorkDetails.adjustments)
    val completedTime = Duration.ofMinutes(unpaidWorkDetails.completedMinutes)
    val remainingMinutesAllowance = requiredTime - completedTime
    if (adjustmentMinutes > remainingMinutesAllowance) {
      badRequest("Credited minutes of '${adjustmentMinutes.formatForUser()}' exceeds the remaining time required of '${remainingMinutesAllowance.formatForUser()}'")
    }
  }
}

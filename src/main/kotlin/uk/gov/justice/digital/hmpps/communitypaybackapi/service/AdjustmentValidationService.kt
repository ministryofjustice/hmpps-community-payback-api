package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidatorWithContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import java.time.Duration
import java.time.LocalDate

@Service
class AdjustmentValidationService(
  private val adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository,
  private val appointmentEntityRepository: AppointmentEntityRepository,
  private val offenderService: OffenderService,
) : ValidatorWithContext<CreateAdjustmentDto, AdjustmentValidationService.AdjustmentValidationContext>() {

  data class AdjustmentValidationContext(
    val upwDetailsId: UnpaidWorkDetailsIdDto,
    val username: String,
  ) : ValidationContext<CreateAdjustmentDto> {
    var reason: AdjustmentReasonEntity? = null
    var appointment: AppointmentEntity? = null
    var unpaidWorkDetails: UnpaidWorkDetailsDto? = null
    var remainingMinutesAllowance: Duration? = null
  }

  override fun configureContext(
    value: CreateAdjustmentDto,
    ctx: AdjustmentValidationContext,
  ): AdjustmentValidationContext {
    ctx.reason = adjustmentReasonEntityRepository.findByIdOrNull(value.adjustmentReasonId)
    ctx.appointment = value.appointmentId?.let { appointmentEntityRepository.findByIdOrNull(it) }
    ctx.unpaidWorkDetails = offenderService.ensureUnpaidWorkDetailsExist(ctx.upwDetailsId, ctx.username)

    if (ctx.unpaidWorkDetails != null) {
      val requiredTime = Duration.ofMinutes(ctx.unpaidWorkDetails!!.requiredMinutes + ctx.unpaidWorkDetails!!.adjustments)
      val completedTime = Duration.ofMinutes(ctx.unpaidWorkDetails!!.completedMinutes)
      ctx.remainingMinutesAllowance = requiredTime - completedTime
    }

    return ctx
  }

  override fun configureRules() {
    rule {
      expect { _, ctx -> ctx.reason != null }
      otherwise {
        this isError "UNKNOWN_ADJUSTMENT_REASON"
        field = "$.adjustmentReasonId"
        data {
          "id" to { value -> value.adjustmentReasonId }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.reason != null }
      assume { _, ctx -> ctx.reason!!.needsLinkToAppointment }
      expect { value -> value.appointmentId != null }
      otherwise {
        this isError "ADJUSTMENT_REASON_NEEDS_APPOINTMENT_ID"
        field = "$.appointmentId"
        data {
          "reasonName" to { _, ctx -> ctx.reason!!.name }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.reason != null }
      assume { _, ctx -> ctx.reason!!.needsLinkToAppointment }
      assume { value -> value.appointmentId != null }
      expect { _, ctx -> ctx.appointment != null }
      otherwise {
        this isError "UNKNOWN_APPOINTMENT"
        field = "$.appointmentId"
        data {
          "id" to { value -> value.appointmentId!! }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.reason != null }
      assume { _, ctx -> !ctx.reason!!.needsLinkToAppointment }
      expect { value -> value.appointmentId == null }
      otherwise {
        this isError "ADJUSTMENT_REASON_DOES_NOT_SUPPORT_APPOINTMENTS"
        field = "$.appointmentId"
        data {
          "reasonName" to { _, ctx -> ctx.reason!!.name }
        }
      }
    }

    rule {
      expect { _, ctx -> ctx.unpaidWorkDetails != null }
      otherwise {
        this isError "COULD_NOT_FIND_UNPAID_WORK_DETAILS"
        field = "$"
        data {
          "crn" to { _, ctx -> ctx.upwDetailsId.crn }
          "deliusEventNumber" to { _, ctx -> ctx.upwDetailsId.deliusEventNumber }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.reason != null }
      expect { value, ctx -> value.minutes <= ctx.reason!!.maxMinutesAllowed }
      otherwise {
        this isError "EXCEEDS_MAXIMUM_ALLOWED_TIME"
        field = "$.minutes"
        data {
          "requestedMinutes" to { value -> value.minutes }
          "maxMinutesAllowed" to { _, ctx -> ctx.reason!!.maxMinutesAllowed }
          "adjustmentReason" to { _, ctx -> ctx.reason!!.name }
        }
      }
    }

    rule {
      expect { value -> value.adjustmentDate?.let { !LocalDate.now().isBefore(it) } }
      otherwise {
        this isError "ADJUSTMENT_DATE_IS_IN_FUTURE"
        field = "$.adjustmentDate"
      }
    }

    rule {
      assume { _, ctx -> ctx.unpaidWorkDetails != null }
      expect { value, ctx -> value.adjustmentDate?.let { !ctx.unpaidWorkDetails!!.sentenceDate.isAfter(it) } }
      otherwise {
        this isError "ADJUSTMENT_DATE_IS_BEFORE_SENTENCE_DATE"
        field = "$.adjustmentDate"
      }
    }

    rule {
      assume { _, ctx -> ctx.remainingMinutesAllowance != null }
      expect { value, ctx -> Duration.ofMinutes(value.minutes.toLong()) <= ctx.remainingMinutesAllowance }
      otherwise {
        this isError "EXCEEDS_REMAINING_REQUIREMENT_TIME"
        field = "$.minutes"
        data {
          "requestedMinutes" to { value -> value.minutes }
          "remainingMinutes" to { _, ctx -> ctx.remainingMinutesAllowance!!.toMinutes() }
        }
      }
    }
  }
}

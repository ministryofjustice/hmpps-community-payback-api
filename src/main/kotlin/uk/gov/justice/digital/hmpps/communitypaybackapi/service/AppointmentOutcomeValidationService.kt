package uk.gov.justice.digital.hmpps.communitypaybackapi.service
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateLengthLessThan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateNotNull
import java.time.Duration
import java.time.LocalDate

@Service
class AppointmentOutcomeValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun ensureUpdateIsValid(
    appointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
  ) {
    validateContactOutcome(appointment, update)
    validateDuration(update)
    validatePenaltyTime(update)
    validateNotes(update)
  }

  fun validateContactOutcome(
    appointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
  ) {
    val code = update.contactOutcomeCode ?: return

    val contactOutcome = contactOutcomeEntityRepository.findByCode(code)
      ?: throw BadRequestException("Contact outcome not found for code $code")

    val appointmentIsInFuture = appointment.date.isAfter(LocalDate.now())
    val attendanceOrEnforcementRecorded = contactOutcome.attended || contactOutcome.enforceable
    if (appointmentIsInFuture && attendanceOrEnforcementRecorded) {
      throw BadRequestException("If the appointment is in the future, only acceptable absences are permitted to be recorded")
    }

    if (contactOutcome.attended) {
      validateNotNull(update.attendanceData) {
        "Attendance data is required for 'attended' contact outcomes"
      }
    }
  }

  fun validateDuration(update: UpdateAppointmentOutcomeDto) {
    if (update.endTime <= update.startTime) {
      throw BadRequestException("End Time '${update.endTime}' must be after Start Time '${update.startTime}'")
    }
  }

  fun validatePenaltyTime(update: UpdateAppointmentOutcomeDto) {
    update.attendanceData?.derivePenaltyMinutesDuration()?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(update.startTime, update.endTime)
      if (penaltyDuration > appointmentDuration) {
        throw BadRequestException("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
      }
    }
  }

  @SuppressWarnings("MagicNumber")
  fun validateNotes(update: UpdateAppointmentOutcomeDto) {
    validateLengthLessThan(update.notes, 4000) { _, _ ->
      "Outcome notes must be fewer than 4000 characters"
    }
  }
}

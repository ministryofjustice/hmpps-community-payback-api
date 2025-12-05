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

  fun validate(
    appointment: AppointmentDto,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    validateContactOutcome(appointment, outcome)
    validateDuration(outcome)
    validatePenaltyTime(outcome)
    validateNotes(outcome)
  }

  fun validateContactOutcome(
    appointment: AppointmentDto,
    outcome: UpdateAppointmentOutcomeDto,
  ) {
    val code = outcome.contactOutcomeCode ?: return

    val contactOutcome = contactOutcomeEntityRepository.findByCode(code)
      ?: throw BadRequestException("Contact outcome not found for code $code")

    val appointmentIsInFuture = appointment.date.isAfter(LocalDate.now())
    val attendanceOrEnforcementRecorded = contactOutcome.attended || contactOutcome.enforceable
    if (appointmentIsInFuture && attendanceOrEnforcementRecorded) {
      throw BadRequestException("If the appointment is in the future, only acceptable absences are permitted to be recorded")
    }

    if (contactOutcome.attended) {
      validateNotNull(outcome.attendanceData) {
        "Attendance data is required for 'attended' contact outcomes"
      }
    }
  }

  fun validateDuration(outcome: UpdateAppointmentOutcomeDto) {
    if (outcome.endTime <= outcome.startTime) {
      throw BadRequestException("End Time '${outcome.endTime}' must be after Start Time '${outcome.startTime}'")
    }
  }

  fun validatePenaltyTime(outcome: UpdateAppointmentOutcomeDto) {
    outcome.attendanceData?.penaltyTime?.duration?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(outcome.startTime, outcome.endTime)
      if (penaltyDuration > appointmentDuration) {
        throw BadRequestException("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
      }
    }
  }

  @SuppressWarnings("MagicNumber")
  fun validateNotes(outcome: UpdateAppointmentOutcomeDto) {
    validateLengthLessThan(outcome.notes, 4000) { _, _ ->
      "Outcome notes must be fewer than 4000 characters"
    }
  }
}

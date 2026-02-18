package uk.gov.justice.digital.hmpps.communitypaybackapi.service
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentCommandDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateLengthLessThan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateNotNull
import java.time.Duration
import java.time.LocalDate

@Service
class AppointmentValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
) {

  fun validateCreate(
    create: CreateAppointmentDto,
  ): Validated<CreateAppointmentDto> {
    validateOutcome(
      appointmentDate = create.date,
      command = create,
    )
    validateDuration(create)
    validatePenaltyTime(create)
    validateNotes(create)

    return Validated(create)
  }

  fun validateUpdate(
    appointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
  ): Validated<UpdateAppointmentOutcomeDto> {
    validateOutcome(
      appointmentDate = appointment.date,
      command = update,
    )
    validateDuration(update)
    validatePenaltyTime(update)
    validateNotes(update)

    return Validated(update)
  }

  private fun validateOutcome(
    appointmentDate: LocalDate,
    command: AppointmentCommandDto,
  ) {
    val code = command.contactOutcomeCode ?: return

    val contactOutcome = contactOutcomeEntityRepository.findByCode(code)
      ?: throw BadRequestException("Contact outcome not found for code $code")

    val appointmentIsInFuture = appointmentDate.isAfter(LocalDate.now())
    val attendanceOrEnforcementRecorded = contactOutcome.attended || contactOutcome.enforceable
    if (appointmentIsInFuture && attendanceOrEnforcementRecorded) {
      throw BadRequestException("If the appointment is in the future, only acceptable absences are permitted to be recorded")
    }

    if (contactOutcome.attended) {
      validateNotNull(command.attendanceData) {
        "Attendance data is required for 'attended' contact outcomes"
      }
    }
  }

  private fun validateDuration(command: AppointmentCommandDto) {
    if (command.endTime <= command.startTime) {
      throw BadRequestException("End Time '${command.endTime}' must be after Start Time '${command.startTime}'")
    }
  }

  private fun validatePenaltyTime(command: AppointmentCommandDto) {
    command.attendanceData?.derivePenaltyMinutesDuration()?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(command.startTime, command.endTime)
      if (penaltyDuration > appointmentDuration) {
        throw BadRequestException("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
      }
    }
  }

  private fun validateNotes(command: AppointmentCommandDto) {
    validateLengthLessThan(command.notes, 4000) { _, _ ->
      "Outcome notes must be fewer than 4000 characters"
    }
  }
}

data class Validated<T>(val value: T)

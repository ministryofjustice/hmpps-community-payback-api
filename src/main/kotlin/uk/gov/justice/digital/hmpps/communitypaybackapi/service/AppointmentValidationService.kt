package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentCommandDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.derivePenaltyMinutesDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateLengthLessThan
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.validateNotNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("ThrowsCount")
@Service
class AppointmentValidationService(
  private val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  private val offenderService: OffenderService,
  private val projectService: ProjectService,
  private val appointmentCalculationService: AppointmentCalculationService,
) {

  fun validateCreate(
    create: CreateAppointmentDto,
  ): ValidatedAppointment<CreateAppointmentDto> {
    val ctx = ValidationContext(
      command = create,
      project = projectService.getProject(create.projectCode),
      contactOutcome = loadContactOutcome(create.contactOutcomeCode),
      unpaidWorkDetails = offenderService.getUnpaidWorkDetails(create.crn, create.deliusEventNumber),
      appointmentDate = create.date,
    )

    ctx.validateDate()
    ctx.validateAvailability()
    ctx.validateOutcome()
    ctx.validateDuration()
    ctx.validatePenaltyTime()
    ctx.validateNotes()
    ctx.validateEteAllowanceRemaining()

    return ValidatedAppointment(
      dto = create,
      minutesToCredit = ctx.calculateMinutesToCredit(),
      contactOutcome = ctx.contactOutcome,
      project = ctx.project,
    )
  }

  fun validateUpdate(
    appointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
  ): ValidatedAppointment<UpdateAppointmentOutcomeDto> {
    val ctx = ValidationContext(
      command = update,
      project = projectService.getProject(appointment.projectCode),
      contactOutcome = loadContactOutcome(update.contactOutcomeCode),
      unpaidWorkDetails = offenderService.getUnpaidWorkDetails(appointment.offender.crn, appointment.deliusEventNumber.toLong()),
      appointmentDate = appointment.date,
      appointmentMinutesAlreadyCredited = appointment.minutesCredited?.let { Duration.ofMinutes(it) } ?: Duration.ZERO,
    )

    ctx.validateOutcome()
    ctx.validateDuration()
    ctx.validatePenaltyTime()
    ctx.validateNotes()
    ctx.validateEteAllowanceRemaining()

    return ValidatedAppointment(
      dto = update,
      minutesToCredit = ctx.calculateMinutesToCredit(),
      contactOutcome = ctx.contactOutcome,
      project = ctx.project,
    )
  }

  private fun ValidationContext.validateDate() {
    val projectEndDateExclusive = project.actualEndDateExclusive

    projectEndDateExclusive?.let {
      if (appointmentDate.onOrAfter(projectEndDateExclusive)) {
        throw BadRequestException("Appointment Date of $appointmentDate must be before project end date $projectEndDateExclusive")
      }
    }

    val sentenceDate = unpaidWorkDetails.sentenceDate
    if (appointmentDate.isBefore(sentenceDate)) {
      throw BadRequestException("Appointment Date of $appointmentDate must be on or after sentence date of $sentenceDate")
    }
  }

  private fun ValidationContext.validateAvailability() {
    val appointmentDayOfWeek = appointmentDate.dayOfWeek

    if (project.availability.none { it.dayOfWeek.toDayOfWeek() == appointmentDayOfWeek }) {
      val availableDays = project.availability.map { it.dayOfWeek }.toSet()
      throw BadRequestException("Project is not available on $appointmentDayOfWeek. Available days are $availableDays")
    }
  }

  private fun ValidationContext.validateOutcome() {
    if (contactOutcome == null) {
      return
    }

    val appointmentIsInFuture = appointmentDate.atTime(command.startTime).isAfter(LocalDateTime.now())
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

  private fun ValidationContext.validateDuration() {
    if (command.endTime <= command.startTime) {
      throw BadRequestException("End Time '${command.endTime}' must be after Start Time '${command.startTime}'")
    }
  }

  private fun ValidationContext.validatePenaltyTime() {
    command.attendanceData?.derivePenaltyMinutesDuration()?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(command.startTime, command.endTime)
      if (penaltyDuration > appointmentDuration) {
        throw BadRequestException("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
      }
    }
  }

  private fun ValidationContext.validateNotes() {
    validateLengthLessThan(command.notes, 4000) { _, _ ->
      "Outcome notes must be fewer than 4000 characters"
    }
  }

  private fun ValidationContext.validateEteAllowanceRemaining() {
    val minutesToCredit = calculateMinutesToCredit()
    if (project.projectType.group == ProjectTypeGroupDto.ETE && minutesToCredit != null) {
      val remainingEteMinutesAllowance = Duration.ofMinutes(unpaidWorkDetails.remainingEteMinutes) + appointmentMinutesAlreadyCredited

      if (minutesToCredit > remainingEteMinutesAllowance) {
        throw BadRequestException("Credited minutes of $minutesToCredit exceeds remaining allowed ETE minutes of $remainingEteMinutesAllowance")
      }
    }
  }

  private fun ValidationContext.calculateMinutesToCredit() = appointmentCalculationService.minutesToCredit(
    contactOutcome = contactOutcome,
    startTime = command.startTime,
    endTime = command.endTime,
    penaltyMinutes = command.attendanceData?.derivePenaltyMinutesDuration(),
  )

  private fun loadContactOutcome(code: String?) = code?.let {
    contactOutcomeEntityRepository.findByCode(it)
      ?: throw BadRequestException("Contact outcome not found for code $code")
  }

  private data class ValidationContext(
    val project: ProjectDto,
    val command: AppointmentCommandDto,
    val contactOutcome: ContactOutcomeEntity?,
    val unpaidWorkDetails: UnpaidWorkDetailsDto,
    val appointmentDate: LocalDate,
    val appointmentMinutesAlreadyCredited: Duration = Duration.ZERO,
  )

  data class ValidatedAppointment<T>(
    val dto: T,
    val minutesToCredit: Duration? = null,
    val contactOutcome: ContactOutcomeEntity? = null,
    val project: ProjectDto,
  ) {
    companion object
  }
}

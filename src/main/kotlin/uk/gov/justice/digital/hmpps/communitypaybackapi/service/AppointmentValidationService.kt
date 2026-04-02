package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.badRequestReferenceNotFound
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validateLengthLessThan
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validateNotNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentCommandDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.derivePenaltyMinutesDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
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
      project = projectService.getProject(create.projectCode) ?: badRequestReferenceNotFound("Project", create.projectCode),
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
      project = projectService.getProject(appointment.projectCode) ?: error("Can't retrieve project ${appointment.projectCode}"),
      contactOutcome = loadContactOutcome(update.contactOutcomeCode),
      unpaidWorkDetails = offenderService.getUnpaidWorkDetails(appointment.offender.crn, appointment.deliusEventNumber),
      appointmentDate = appointment.date,
      appointmentMinutesAlreadyCredited = appointment.minutesCredited?.let { Duration.ofMinutes(it) } ?: Duration.ZERO,
      existingContactOutcome = loadContactOutcome(appointment.contactOutcomeCode),
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
        badRequest("Appointment Date of ${appointmentDate.formatForUser()} must be before project end date ${projectEndDateExclusive.formatForUser()}")
      }
    }

    val sentenceDate = unpaidWorkDetails.sentenceDate
    if (appointmentDate.isBefore(sentenceDate)) {
      badRequest("Appointment Date of ${appointmentDate.formatForUser()} must be on or after sentence date of ${sentenceDate.formatForUser()}")
    }
  }

  private fun ValidationContext.validateAvailability() {
    val appointmentDayOfWeek = appointmentDate.dayOfWeek
    val availableDays = project.availability.map { it.dayOfWeek.toDayOfWeek() }

    if (!availableDays.contains(appointmentDayOfWeek)) {
      val availableDaysString = availableDays.joinToString(separator = ", ") { it.formatForUser() }
      badRequest("Project is not available on ${appointmentDayOfWeek.formatForUser()}. Available days are $availableDaysString")
    }
  }

  private fun ValidationContext.validateOutcome() {
    if (contactOutcome == null) {
      if (appointmentIsInPast()) {
        badRequest("As the appointment is now complete a contact outcome is required")
      }
    } else {
      if (appointmentIsInFuture() && (contactOutcome.attended || contactOutcome.enforceable)) {
        badRequest("As the appointment is in the future only acceptable absence outcomes can be recorded")
      }

      if (contactOutcome.attended) {
        validateNotNull(command.attendanceData) {
          "Attendance data is required for contact outcomes that indicate attendance"
        }
      }
    }

    if (existingContactOutcome != null && contactOutcome != existingContactOutcome) {
      badRequest("The existing contact outcome of '${existingContactOutcome.name}' cannot be modified")
    }
  }

  private fun ValidationContext.validateDuration() {
    if (command.endTime <= command.startTime) {
      badRequest("End Time '${command.endTime}' must be after Start Time '${command.startTime}'")
    }
  }

  private fun ValidationContext.validatePenaltyTime() {
    command.attendanceData?.derivePenaltyMinutesDuration()?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(command.startTime, command.endTime)
      if (penaltyDuration > appointmentDuration) {
        badRequest("Penalty duration '${penaltyDuration.formatForUser()}' is greater than appointment duration '${appointmentDuration.formatForUser()}'")
      }
    }
  }

  private fun ValidationContext.validateNotes() {
    validateLengthLessThan(command.notes, 4000) { _, _ ->
      "Notes must be fewer than 4000 characters"
    }
  }

  private fun ValidationContext.validateEteAllowanceRemaining() {
    val minutesToCredit = calculateMinutesToCredit()
    if (project.projectType.group == ProjectTypeGroupDto.ETE && minutesToCredit != null) {
      val remainingEteMinutesAllowance = Duration.ofMinutes(unpaidWorkDetails.remainingEteMinutes) + appointmentMinutesAlreadyCredited

      if (minutesToCredit > remainingEteMinutesAllowance) {
        badRequest("Credited minutes of '${minutesToCredit.formatForUser()}' exceeds remaining allowed ETE time of '${remainingEteMinutesAllowance.formatForUser()}'")
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
    contactOutcomeEntityRepository.findByCode(it) ?: badRequest("Contact outcome not found for code '$code'")
  }

  private data class ValidationContext(
    val project: ProjectDto,
    val command: AppointmentCommandDto,
    val contactOutcome: ContactOutcomeEntity?,
    val unpaidWorkDetails: UnpaidWorkDetailsDto,
    val appointmentDate: LocalDate,
    val appointmentMinutesAlreadyCredited: Duration = Duration.ZERO,
    val existingContactOutcome: ContactOutcomeEntity? = null,
  ) {
    fun appointmentIsInFuture() = appointmentDate.atTime(command.startTime).isAfter(LocalDateTime.now())
    fun appointmentIsInPast() = appointmentDate.atTime(command.endTime).isBefore(LocalDateTime.now())
  }

  data class ValidatedAppointment<T>(
    val dto: T,
    val minutesToCredit: Duration? = null,
    val contactOutcome: ContactOutcomeEntity? = null,
    val project: ProjectDto,
  ) {
    companion object
  }
}

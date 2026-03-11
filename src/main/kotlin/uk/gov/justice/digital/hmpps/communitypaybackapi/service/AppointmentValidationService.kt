package uk.gov.justice.digital.hmpps.communitypaybackapi.service
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentCommandDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
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
) {

  fun validateCreate(
    create: CreateAppointmentDto,
  ): Validated<CreateAppointmentDto> {
    val ctx = ValidationContext(
      command = create,
      project = projectService.getProject(create.projectCode),
      contactOutcome = loadContactOutcome(create.contactOutcomeCode),
      unpaidWorkDetails = offenderService.getUnpaidWorkDetails(create.crn, create.deliusEventNumber),
    )

    validateDate(ctx, create)
    validateAvailability(ctx, create)
    validateOutcome(
      ctx = ctx,
      appointmentDate = create.date,
    )
    validateDuration(ctx)
    validatePenaltyTime(ctx)
    validateNotes(ctx)

    return Validated(create)
  }

  fun validateUpdate(
    appointment: AppointmentDto,
    update: UpdateAppointmentOutcomeDto,
  ): Validated<UpdateAppointmentOutcomeDto> {
    val ctx = ValidationContext(
      command = update,
      project = projectService.getProject(appointment.projectCode),
      contactOutcome = loadContactOutcome(update.contactOutcomeCode),
      unpaidWorkDetails = offenderService.getUnpaidWorkDetails(appointment.offender.crn, appointment.deliusEventNumber.toLong()),
    )

    validateOutcome(
      ctx = ctx,
      appointmentDate = appointment.date,
    )
    validateDuration(ctx)
    validatePenaltyTime(ctx)
    validateNotes(ctx)

    return Validated(update)
  }

  private fun validateDate(
    ctx: ValidationContext,
    appointment: CreateAppointmentDto,
  ) {
    val appointmentDate = appointment.date
    val projectEndDateExclusive = ctx.project.actualEndDateExclusive

    projectEndDateExclusive?.let {
      if (appointmentDate.onOrAfter(projectEndDateExclusive)) {
        throw BadRequestException("Appointment Date of $appointmentDate must be before project end date $projectEndDateExclusive")
      }
    }

    val sentenceDate = ctx.unpaidWorkDetails.sentenceDate
    if (appointmentDate.isBefore(sentenceDate)) {
      throw BadRequestException("Appointment Date of $appointmentDate must be on or after sentence date of $sentenceDate")
    }
  }

  private fun validateAvailability(
    ctx: ValidationContext,
    appointment: CreateAppointmentDto,
  ) {
    val appointmentDayOfWeek = appointment.date.dayOfWeek

    if (ctx.project.availability.none { it.dayOfWeek.toDayOfWeek() == appointmentDayOfWeek }) {
      val availableDays = ctx.project.availability.map { it.dayOfWeek }.toSet()
      throw BadRequestException("Project is not available on $appointmentDayOfWeek. Available days are $availableDays")
    }
  }

  private fun validateOutcome(
    ctx: ValidationContext,
    appointmentDate: LocalDate,
  ) {
    if (ctx.contactOutcome == null) {
      return
    }

    val appointmentIsInFuture = appointmentDate.atTime(ctx.command.startTime).isAfter(LocalDateTime.now())
    val attendanceOrEnforcementRecorded = ctx.contactOutcome.attended || ctx.contactOutcome.enforceable
    if (appointmentIsInFuture && attendanceOrEnforcementRecorded) {
      throw BadRequestException("If the appointment is in the future, only acceptable absences are permitted to be recorded")
    }

    if (ctx.contactOutcome.attended) {
      validateNotNull(ctx.command.attendanceData) {
        "Attendance data is required for 'attended' contact outcomes"
      }
    }
  }

  private fun validateDuration(ctx: ValidationContext) {
    if (ctx.command.endTime <= ctx.command.startTime) {
      throw BadRequestException("End Time '${ctx.command.endTime}' must be after Start Time '${ctx.command.startTime}'")
    }
  }

  private fun validatePenaltyTime(ctx: ValidationContext) {
    ctx.command.attendanceData?.derivePenaltyMinutesDuration()?.let { penaltyDuration ->
      val appointmentDuration = Duration.between(ctx.command.startTime, ctx.command.endTime)
      if (penaltyDuration > appointmentDuration) {
        throw BadRequestException("Penalty duration '$penaltyDuration' is greater than appointment duration '$appointmentDuration'")
      }
    }
  }

  private fun validateNotes(ctx: ValidationContext) {
    validateLengthLessThan(ctx.command.notes, 4000) { _, _ ->
      "Outcome notes must be fewer than 4000 characters"
    }
  }

  private fun loadContactOutcome(code: String?) = code?.let {
    contactOutcomeEntityRepository.findByCode(it)
      ?: throw BadRequestException("Contact outcome not found for code $code")
  }

  private data class ValidationContext(
    val project: ProjectDto,
    val command: AppointmentCommandDto,
    val contactOutcome: ContactOutcomeEntity?,
    val unpaidWorkDetails: UnpaidWorkDetailsDto,
  )
}

data class Validated<T>(val value: T)

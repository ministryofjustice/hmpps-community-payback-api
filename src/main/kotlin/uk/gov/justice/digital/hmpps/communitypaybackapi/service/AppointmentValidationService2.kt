package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidatorWithContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentCommandDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.PickUpLocationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.derivePenaltyMinutesDuration
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService2.AppointmentValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService2.AppointmentValidationContext.Create
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService2.AppointmentValidationContext.Update
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDayOfWeek
import java.time.Duration
import java.time.LocalDateTime

abstract class AppointmentValidationService2<T : AppointmentCommandDto, TContext : AppointmentValidationContext>(
  protected val contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  protected val offenderService: OffenderService,
  protected val projectService: ProjectService,
  protected val providerService: ProviderService,
  protected val appointmentCalculationService: AppointmentCalculationService,
) : ValidatorWithContext<T, TContext>() {

  sealed class AppointmentValidationContext : ValidationContext<AppointmentCommandDto> {
    var project: ProjectDto? = null
    var contactOutcome: FindResult<ContactOutcomeEntity> = FindResult.None()
    var pickUpLocation: FindResult<PickUpLocationDto> = FindResult.None()
    var unpaidWorkDetails: UnpaidWorkDetailsDto? = null
    var timeToCredit: Duration? = null

    open val appointmentTimeAlreadyCredited: Duration
      get() = Duration.ZERO

    class Create : AppointmentValidationContext()

    class Update(val existingAppointment: AppointmentDto) : AppointmentValidationContext() {
      override val appointmentTimeAlreadyCredited: Duration
        get() = existingAppointment.minutesCredited?.let { Duration.ofMinutes(it) } ?: Duration.ZERO
    }
  }

  sealed interface FindResult<T> {
    class None<T> : FindResult<T>
    class Unknown<T> : FindResult<T>
    data class Found<T>(private val inner: T) : FindResult<T> {
      override val value: T?
        get() = this.inner
    }

    val value: T?
      get() = null

    companion object {
      fun <T> find(code: String?, findFunc: (String) -> T?): FindResult<T> = code?.let {
        when (val entity = findFunc(it)) {
          null -> Unknown()
          else -> Found(entity)
        }
      } ?: None()
    }
  }

  final override fun configureContext(value: T, ctx: TContext): TContext {
    val upwDetailsId = getUpwDetailsId(value, ctx)
    val project = getProject(value, ctx)

    if (project != null) {
      ctx.project = project
      ctx.pickUpLocation = findPickUpLocation(project, getPickUpLocationCode(value, ctx))
    }

    ctx.contactOutcome = findContactOutcome(value.contactOutcomeCode)
    ctx.unpaidWorkDetails = offenderService.getUnpaidWorkDetails(upwDetailsId)
    ctx.timeToCredit = appointmentCalculationService.minutesToCredit(
      contactOutcome = ctx.contactOutcome.value,
      startTime = value.startTime,
      endTime = value.endTime,
      penaltyMinutes = value.attendanceData?.derivePenaltyMinutesDuration(),
    )

    return ctx
  }

  override fun configureRules() {
    rule {
      expect { _, ctx -> ctx.project != null }
      otherwise {
        this isError "UNKNOWN_PROJECT_CODE"
        field = "$.projectCode"
        data {
          "code" to { value, ctx -> value.getProjectCode(ctx) }
        }
      }
    }

    rule {
      expect { _, ctx -> ctx.unpaidWorkDetails != null }
      otherwise {
        this isError "COULD_NOT_FIND_UNPAID_WORK_DETAILS"
        field = "$..[\"crn\", \"deliusEventNumber\"]"
        data {
          "crn" to { value, ctx -> value.getCrn(ctx) }
          "deliusEventNumber" to { value, ctx -> value.getDeliusEventNumber(ctx) }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.project?.actualEndDateExclusive != null }
      expect { value, ctx -> value.date.isBefore(ctx.project!!.actualEndDateExclusive!!) }
      otherwise {
        this isError "APPOINTMENT_DATE_IS_NOT_BEFORE_END_OF_PROJECT"
        field = "$.date"
        data {
          "date" to { value -> value.date }
          "projectEndDateExclusive" to { _, ctx -> ctx.project!!.actualEndDateExclusive!! }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.unpaidWorkDetails != null }
      expect { value, ctx -> value.date.onOrAfter(ctx.unpaidWorkDetails!!.sentenceDate) }
      otherwise {
        this isError "APPOINTMENT_DATE_IS_BEFORE_SENTENCE_DATE"
        field = "$.date"
        data {
          "date" to { value -> value.date }
          "sentenceDate" to { _, ctx -> ctx.unpaidWorkDetails!!.sentenceDate }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.project != null }
      expect { value, ctx -> ctx.project!!.availableDays.contains(value.date.dayOfWeek) }
      otherwise {
        this isError "PROJECT_NOT_AVAILABLE_ON_REQUESTED_DAY_OF_WEEK"
        field = "$.date"
        data {
          "requestedDayOfWeek" to { value -> value.date.dayOfWeek }
          "availableDays" to { _, ctx -> ctx.project!!.availableDays }
        }
      }
    }

    rule {
      assume { value -> value.contactOutcomeCode != null }
      expect { _, ctx -> ctx.contactOutcome !is FindResult.Unknown }
      otherwise {
        this isError "UNKNOWN_CONTACT_OUTCOME"
        field = "$.contactOutcomeCode"
        data {
          "code" to { value -> value.contactOutcomeCode!! }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.contactOutcome is FindResult.None }
      expect { value -> value.date.atTime(value.endTime).onOrAfter(LocalDateTime.now()) }
      otherwise {
        this isError "PAST_APPOINTMENT_REQUIRES_CONTACT_OUTCOME"
        field = "$.contactOutcomeCode"
      }
    }

    rule {
      assume { _, ctx -> ctx.contactOutcome is FindResult.Found }
      assume { value -> value.date.atTime(value.startTime).isAfter(LocalDateTime.now()) }
      expect { _, ctx -> ctx.contactOutcome.value!!.let { !it.enforceable && !it.attended } }
      otherwise {
        this isError "FUTURE_APPOINTMENT_ONLY_ALLOWS_ACCEPTABLE_ABSENCE_OUTCOMES"
        field = "$.contactOutcomeCode"
      }
    }

    rule {
      assume { _, ctx -> ctx.contactOutcome is FindResult.Found }
      assume { _, ctx -> ctx.contactOutcome.value!!.attended }
      expect { value -> value.attendanceData != null }
      otherwise {
        this isError "ATTENDED_CONTACT_OUTCOME_REQUIRES_ATTENDANCE_DATA"
        field = "$.attendanceData"
      }
    }

    rule {
      expect { value -> value.endTime.isAfter(value.startTime) }
      otherwise {
        this isError "END_TIME_NOT_AFTER_START_TIME"
        field = "$.endTime"
        data {
          "startTime" to { value -> value.startTime }
          "endTime" to { value -> value.endTime }
        }
      }
    }

    rule {
      assume { value -> value.penaltyDuration != null }
      expect { value -> value.penaltyDuration!! <= value.appointmentDuration }
      otherwise {
        this isError "PENALTY_DURATION_EXCEEDS_APPOINTMENT_DURATION"
        field = "$.penaltyMinutes"
        data {
          "penaltyDuration" to { value -> value.penaltyDuration!! }
          "appointmentDuration" to { value -> value.appointmentDuration }
        }
      }
    }

    rule {
      expect { value -> value.notes?.let { it.length <= 4000 } }
      otherwise {
        this isError "NOTES_TOO_LONG"
        field = "$.notes"
        data {
          "length" to { value -> value.notes!!.length }
          "maxLength" to 4000
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.timeToCredit != null }
      assume { _, ctx -> ctx.unpaidWorkDetails != null }
      expect { _, ctx -> ctx.timeToCredit!! <= ctx.remainingRequirementTime }
      otherwise {
        this isError "CREDITED_TIME_EXCEEDS_REMAINING_REQUIREMENT_TIME"
        field = "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]"
        data {
          "timeToCredit" to { _, ctx -> ctx.timeToCredit!! }
          "remainingRequirementTime" to { _, ctx -> ctx.remainingRequirementTime }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.project?.projectType?.group == ProjectTypeGroupDto.ETE }
      assume { _, ctx -> ctx.timeToCredit != null }
      assume { _, ctx -> ctx.unpaidWorkDetails != null }
      expect { _, ctx -> ctx.timeToCredit!! <= ctx.remainingEteTime }
      otherwise {
        this isError "CREDITED_ETE_TIME_EXCEEDS_REMAINING_ETE_TIME"
        field = "$..[\"startTime\", \"endTime\", \"penaltyMinutes\"]"
        data {
          "timeToCredit" to { _, ctx -> ctx.timeToCredit!! }
          "remainingEteTime" to { _, ctx -> ctx.remainingEteTime }
        }
      }
    }

    rule {
      assume { _, ctx -> ctx.project != null }
      assume { _, ctx -> ctx.pickUpLocation !is FindResult.None }
      expect { _, ctx -> ctx.pickUpLocation is FindResult.Found }
      otherwise {
        this isError "UNKNOWN_PICKUP_LOCATION"
        field = "$.pickUpLocationCode"
        data {
          "projectTeamCode" to { _, ctx -> ctx.project!!.teamCode }
          "locationCode" to { value, ctx -> getPickUpLocationCode(value, ctx)!! }
        }
      }
    }
  }

  protected abstract fun T.getProjectCode(ctx: TContext): String
  protected abstract fun T.getCrn(ctx: TContext): String
  protected abstract fun T.getDeliusEventNumber(ctx: TContext): Int

  private val ProjectDto.availableDays
    get() = this.availability.map { it.dayOfWeek.toDayOfWeek() }

  private val T.penaltyDuration
    get() = this.attendanceData?.derivePenaltyMinutesDuration()

  private val T.appointmentDuration
    get() = Duration.between(this.startTime, this.endTime)

  private val TContext.remainingRequirementTime: Duration
    get() {
      val requiredTime = Duration.ofMinutes(unpaidWorkDetails!!.requiredMinutes + unpaidWorkDetails!!.adjustments)
      val completedTime = Duration.ofMinutes(unpaidWorkDetails!!.completedMinutes)

      return requiredTime - completedTime + appointmentTimeAlreadyCredited
    }

  private val TContext.remainingEteTime: Duration
    get() = Duration.ofMinutes(unpaidWorkDetails!!.remainingEteMinutes) + appointmentTimeAlreadyCredited

  protected abstract fun getUpwDetailsId(value: T, ctx: TContext): UnpaidWorkDetailsIdDto
  protected abstract fun getProject(value: T, ctx: TContext): ProjectDto?
  protected abstract fun getPickUpLocationCode(value: T, ctx: TContext): String?

  protected fun findContactOutcome(code: String?) = FindResult.find(code) { contactOutcomeEntityRepository.findByCode(it) }
  protected fun findPickUpLocation(project: ProjectDto, locationCode: String?) = FindResult.find(locationCode) { providerService.getPickupLocation(project.getTeamId(), it) }
}

@Service
class CreateAppointmentValidationService(
  contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  offenderService: OffenderService,
  projectService: ProjectService,
  providerService: ProviderService,
  appointmentCalculationService: AppointmentCalculationService,
) : AppointmentValidationService2<CreateAppointmentDto, AppointmentValidationContext.Create>(contactOutcomeEntityRepository, offenderService, projectService, providerService, appointmentCalculationService) {
  override fun getUpwDetailsId(value: CreateAppointmentDto, ctx: Create): UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(value.crn, value.deliusEventNumber)
  override fun getProject(value: CreateAppointmentDto, ctx: Create): ProjectDto? = projectService.getProject(value.projectCode)
  override fun getPickUpLocationCode(value: CreateAppointmentDto, ctx: Create): String? = value.pickUpLocationCode

  override fun CreateAppointmentDto.getProjectCode(ctx: AppointmentValidationContext.Create) = this.projectCode
  override fun CreateAppointmentDto.getCrn(ctx: AppointmentValidationContext.Create) = this.crn
  override fun CreateAppointmentDto.getDeliusEventNumber(ctx: AppointmentValidationContext.Create) = this.deliusEventNumber
}

@Service
class UpdateAppointmentValidationService(
  contactOutcomeEntityRepository: ContactOutcomeEntityRepository,
  offenderService: OffenderService,
  projectService: ProjectService,
  providerService: ProviderService,
  appointmentCalculationService: AppointmentCalculationService,
) : AppointmentValidationService2<UpdateAppointmentDto, AppointmentValidationContext.Update>(contactOutcomeEntityRepository, offenderService, projectService, providerService, appointmentCalculationService) {
  override fun configureRules() {
    super.configureRules()

    rule {
      assume { _, ctx -> ctx.existingAppointment.sensitive == true }
      expect { value -> value.sensitive == true }
      otherwise {
        this isError "APPOINTMENT_IS_SENSITIVE"
        field = "$.sensitive"
      }
    }
  }

  override fun getUpwDetailsId(value: UpdateAppointmentDto, ctx: Update): UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(ctx.existingAppointment.offender.crn, ctx.existingAppointment.deliusEventNumber)
  override fun getProject(value: UpdateAppointmentDto, ctx: Update): ProjectDto? = projectService.getProject(value.resolveProjectCode(ctx.existingAppointment))
  override fun getPickUpLocationCode(value: UpdateAppointmentDto, ctx: Update): String? = ctx.existingAppointment.pickUpData?.pickupLocation?.deliusCode

  override fun UpdateAppointmentDto.getProjectCode(ctx: AppointmentValidationContext.Update) = this.resolveProjectCode(ctx.existingAppointment)
  override fun UpdateAppointmentDto.getCrn(ctx: AppointmentValidationContext.Update) = ctx.existingAppointment.offender.crn
  override fun UpdateAppointmentDto.getDeliusEventNumber(ctx: AppointmentValidationContext.Update) = ctx.existingAppointment.deliusEventNumber
}

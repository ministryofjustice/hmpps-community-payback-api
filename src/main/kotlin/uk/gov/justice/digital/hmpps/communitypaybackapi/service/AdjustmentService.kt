package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.support.PageableUtils
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.IdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.formatForUser
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator.DeleteAdjustmentProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService.AdjustmentValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdjustmentService(
  private val adjustmentValidationService: AdjustmentValidationService,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val clock: Clock,
  private val springEventPublisher: SpringEventPublisher,
  private val adjustmentIdGenerator: AdjustmentIdGenerator,
  private val adjustmentEventEntityRepository: AdjustmentEventEntityRepository,
) {
  private val logger = LoggerFactory.getLogger(AdjustmentService::class.java)

  fun getAdjustments(crn: String, eventNumber: Int) = communityPaybackAndDeliusClient.getAdjustments(crn, eventNumber).adjustments.map { it.toDto() }

  fun getAdjustments(crn: String, eventNumber: Int, pageable: Pageable): Page<AdjustmentDto> {
    val allAdjustments = communityPaybackAndDeliusClient.getAdjustments(crn, eventNumber).adjustments

    val offset = PageableUtils.getOffsetAsInteger(pageable)

    val comparator = pageable.sort.mapNotNull { order ->
      when (order.property) {
        "type" -> Comparator.comparing<NDAdjustment, NDAdjustmentType> { it.type }
        "date" -> Comparator.comparing<NDAdjustment, LocalDate> { it.date }
        "reason" -> Comparator.comparing<NDAdjustment, String> { it.reason.name }
        "minutes" -> Comparator.comparing<NDAdjustment, Int> { it.minutes }
        else -> null
      }?.let {
        when (order.direction) {
          Sort.Direction.ASC -> it
          Sort.Direction.DESC -> it.reversed()
        }
      }
    }.reduce { acc, comparator -> acc.then(comparator) }

    val adjustments = allAdjustments.sortedWith(comparator).drop(offset).take(pageable.pageSize).map { it.toDto() }

    return PageImpl(adjustments, pageable, allAdjustments.size.toLong())
  }

  @Transactional
  fun createAdjustment(
    upwDetailsId: UnpaidWorkDetailsIdDto,
    createAdjustment: CreateAdjustmentDto,
    username: String,
  ): AdjustmentDto {
    val validationContext = AdjustmentValidationContext(upwDetailsId, username)
    val validationResult = adjustmentValidationService.validate(createAdjustment, validationContext)
    if (validationResult.hasErrors) {
      throwValidationError(validationResult.errors[0])
    }

    val adjustmentId = adjustmentIdGenerator.generateId(createAdjustment)

    deleteOrphanedAdjustmentIfExists(adjustmentId)

    val (crn, deliusEventNumber) = upwDetailsId
    val adjustmentDate = createAdjustment.adjustmentDate ?: LocalDate.now(clock)

    val deliusAdjustmentId = communityPaybackAndDeliusClient.postAdjustments(
      username,
      listOf(
        createAdjustment.toNDAdjustmentRequest(
          crn = crn,
          deliusEventNumber = deliusEventNumber,
          reason = validationContext.reason!!,
          reference = adjustmentId,
          dateOfAdjustment = adjustmentDate,
        ),
      ),
    ).single().id

    springEventPublisher.publishEvent(
      AdjustmentCreatedEvent(
        id = adjustmentId,
        createDto = createAdjustment,
        appointmentEntity = validationContext.appointment,
        reason = validationContext.reason!!,
        deliusAdjustmentId = deliusAdjustmentId,
        trigger = AdjustmentEventTrigger(
          triggeredAt = OffsetDateTime.now(clock),
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = validationContext.appointment?.id.toString(),
        ),
        adjustmentDate = adjustmentDate,
      ),
    )

    return communityPaybackAndDeliusClient.getAdjustment(adjustmentId).toDto()
  }

  @Suppress("detekt:ThrowsCount")
  private fun throwValidationError(error: ValidationResultItem) {
    when (error.code) {
      "UNKNOWN_ADJUSTMENT_REASON" -> throw BadRequestException("Adjustment Reason not found for ID '${error.data["id"]}'")
      "ADJUSTMENT_REASON_NEEDS_APPOINTMENT_ID" -> throw BadRequestException("Adjustment reason '${error.data["reasonName"]}' needs an appointment ID")
      "UNKNOWN_APPOINTMENT" -> throw BadRequestException("Appointment not found for ID '${error.data["id"]}'")
      "ADJUSTMENT_REASON_DOES_NOT_SUPPORT_APPOINTMENTS" -> throw BadRequestException("Adjustment reason '${error.data["reasonName"]}' does not support linking to appointments")
      "COULD_NOT_FIND_UNPAID_WORK_DETAILS" -> throw BadRequestException("Unpaid Work Details not found for CRN ${error.data["crn"]} and event number ${error.data["deliusEventNumber"]}")
      "EXCEEDS_MAXIMUM_ALLOWED_TIME" -> {
        val requestedDuration = Duration.ofMinutes((error.data["requestedMinutes"] as Int).toLong())
        val maximumDuration = Duration.ofMinutes((error.data["maxMinutesAllowed"] as Int).toLong())
        throw BadRequestException("Requested adjustment of '${requestedDuration.formatForUser()}' exceeds the maximum allowed time '${maximumDuration.formatForUser()}' for adjustment reason '${error.data["reason"]}'")
      }
      "ADJUSTMENT_DATE_IS_IN_FUTURE" -> throw BadRequestException("Adjustment date must not be in the future")
      "ADJUSTMENT_DATE_IS_BEFORE_SENTENCE_DATE" -> throw BadRequestException("Adjustment date must not be before the sentence date")
      "EXCEEDS_REMAINING_REQUIREMENT_TIME" -> {
        val requestedDuration = Duration.ofMinutes((error.data["requestedMinutes"] as Int).toLong())
        val remainingDuration = Duration.ofMinutes(error.data["remainingMinutes"] as Long)
        throw BadRequestException("Credited minutes of '${requestedDuration.formatForUser()}' exceeds the remaining time required of '${remainingDuration.formatForUser()}'")
      }
    }
  }

  @Transactional
  fun deleteAdjustment(adjustmentId: UUID, username: String): DeleteAdjustmentResult {
    val eventId = adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId))

    val eventToDelete = adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)
    if (eventToDelete == null) {
      logger.warn("Could not find adjustment event with ID {} in adjustment_events table", adjustmentId)
      return DeleteAdjustmentResult.NotFound
    }

    try {
      communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
    } catch (_: WebClientResponseException.NotFound) {
      logger.warn("Could not delete adjustment with reference {} from NDelius", adjustmentId)
      return DeleteAdjustmentResult.NotFound
    } catch (e: WebClientResponseException) {
      return DeleteAdjustmentResult.Failed(e)
    }

    springEventPublisher.publishEvent(
      AdjustmentDeletedEvent(
        id = eventId,
        eventToDelete = eventToDelete,
        trigger = AdjustmentEventTrigger(
          triggeredAt = OffsetDateTime.now(clock),
          triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
          triggeredBy = username,
        ),
      ),
    )

    return DeleteAdjustmentResult.Success
  }

  @EventListener
  fun rollbackAdjustment(
    rollbackEvent: CommunityPaybackSpringEvent.NDeliusRollbackRequired,
  ) {
    val event = rollbackEvent.event
    if (event is AdjustmentCreatedEvent) {
      communityPaybackAndDeliusClient.deleteAdjustment(event.id)
    }
  }

  private fun deleteOrphanedAdjustmentIfExists(reference: UUID) {
    try {
      communityPaybackAndDeliusClient.deleteAdjustment(reference)
    } catch (e: WebClientResponseException) {
      if (e.statusCode != HttpStatus.NOT_FOUND) {
        throw e
      }
    }
  }
}

sealed interface DeleteAdjustmentResult {
  object NotFound : DeleteAdjustmentResult
  data class Failed(val exception: WebClientResponseException) : DeleteAdjustmentResult
  object Success : DeleteAdjustmentResult
}

interface AdjustmentIdGenerator {
  fun generateId(createAdjustment: CreateAdjustmentDto): UUID
  fun generateId(deleteAdjustment: DeleteAdjustmentProperties): UUID
  data class DeleteAdjustmentProperties(val id: UUID)
}

@Service
class DefaultAdjustmentIdGenerator : AdjustmentIdGenerator {
  override fun generateId(createAdjustment: CreateAdjustmentDto) = IdGenerator(CreateAdjustmentDto::class).generateId(createAdjustment)
  override fun generateId(deleteAdjustment: AdjustmentIdGenerator.DeleteAdjustmentProperties) = IdGenerator(AdjustmentIdGenerator.DeleteAdjustmentProperties::class).generateId(deleteAdjustment)
}

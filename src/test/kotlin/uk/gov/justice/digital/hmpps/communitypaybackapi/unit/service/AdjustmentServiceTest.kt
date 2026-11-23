package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentPostResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentReferenceRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentFilterTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.config.ClockConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentIdGenerator.DeleteAdjustmentProperties
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentValidationService.AdjustmentValidationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.DeleteAdjustmentResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AdjustmentDeletedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SpringEventPublisher
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDAdjustmentRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class AdjustmentServiceTest {

  @RelaxedMockK
  private lateinit var adjustmentValidationService: AdjustmentValidationService

  @RelaxedMockK
  private lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  private lateinit var springEventPublisher: SpringEventPublisher

  @RelaxedMockK
  lateinit var adjustmentIdGenerator: AdjustmentIdGenerator

  @RelaxedMockK
  lateinit var adjustmentEntityRepository: AdjustmentEntityRepository

  @RelaxedMockK
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @RelaxedMockK
  lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  val clock: Clock = ClockConfiguration.MutableClock(Instant.now())

  @InjectMockKs
  private lateinit var service: AdjustmentService

  companion object {
    const val CRN: String = "CRN123"
    const val EVENT_NUMBER: Int = 68
    val UNPAID_WORK_DETAILS: UnpaidWorkDetailsIdDto = UnpaidWorkDetailsIdDto(CRN, EVENT_NUMBER)
    const val USERNAME = "username"
  }

  @Nested
  inner class GetAdjustment {
    @Test
    fun success() {
      val adjustment = NDAdjustment.valid()
      val id = adjustment.reference!!
      every { communityPaybackAndDeliusClient.getAdjustment(id) } returns adjustment

      assertThat(service.getAdjustment(id)).isEqualTo(adjustment.toDto())
      verify(exactly = 1) { communityPaybackAndDeliusClient.getAdjustment(id) }
    }

    @Test
    fun `returns null when adjustment not found`() {
      val id = UUID.randomUUID()
      val error = WebClientResponseExceptionFactory.notFound()
      every { communityPaybackAndDeliusClient.getAdjustment(id) } throws error

      assertThat(service.getAdjustment(id)).isNull()
    }

    @Test
    fun `propagates other upstream errors`() {
      val id = UUID.randomUUID()
      val error = WebClientResponseExceptionFactory.badRequest("Some error")
      every { communityPaybackAndDeliusClient.getAdjustment(id) } throws error

      assertThatThrownBy { service.getAdjustment(id) }.isSameAs(error)
    }
  }

  @Nested
  inner class GetAdjustments {
    @Test
    fun `filters other adjustments before pagination and counts only matches`() {
      val adjustments = listOf(
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 8, 8), reason = NDNameCode("Travel Time", "TTX")),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 7, 7), reason = NDNameCode("Other", "OTH")),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 6, 6), reason = NDNameCode("Other", "OTH")),
      )
      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(adjustments)

      val result = service.getAdjustments("X123456", 1, PageRequest.of(1, 1, Sort.by("date").descending()), AdjustmentFilterTypeDto.OTHER)

      assertThat(result.content).containsExactly(adjustments[2].toDto())
      assertThat(result.totalElements).isEqualTo(2)
      assertThat(result.totalPages).isEqualTo(2)
    }

    @Test
    fun `filters travel time and returns an empty page when no adjustments match`() {
      val travelTime = NDAdjustment.valid().copy(reason = NDNameCode("Travel Time", "TTX"))
      val other = NDAdjustment.valid().copy(reason = NDNameCode("Other", "OTH"))
      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(listOf(travelTime, other))
      val pageable = PageRequest.of(0, 10, Sort.by("date"))

      val result = service.getAdjustments("X123456", 1, pageable, AdjustmentFilterTypeDto.TRAVEL_TIME)

      assertThat(result.content).containsExactly(travelTime.toDto())
      assertThat(result.totalElements).isEqualTo(1)

      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(listOf(travelTime))
      val emptyResult = service.getAdjustments("X123456", 1, pageable, AdjustmentFilterTypeDto.OTHER)

      assertThat(emptyResult.content).isEmpty()
      assertThat(emptyResult.totalElements).isZero()
    }

    @Test
    fun success() {
      val adjustments = listOf(
        NDAdjustment.valid(),
        NDAdjustment.valid(),
      )

      every { communityPaybackAndDeliusClient.getAdjustments(any(), any()) } returns NDAdjustmentResponse(
        adjustments = adjustments,
      )

      val results = service.getAdjustments("X123456", 1)

      assertThat(results).hasSameElementsAs(adjustments.map { it.toDto() })
    }

    @ParameterizedTest
    @ArgumentsSource(PageableArgumentsProvider::class)
    fun `when pageable is provided, then apply paging and sorting`(sortField: String, direction: Sort.Direction, page: Int, expectedIds: List<UUID>) {
      val adjustments = listOf(
        NDAdjustment(
          id = 1L,
          reference = UUID(1L, 1L),
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.of(2026, 1, 1),
          reason = NDNameCode("Reason 06", "R06"),
          minutes = 30,
        ),
        NDAdjustment(
          id = 2L,
          reference = UUID(2L, 2L),
          type = NDAdjustmentType.POSITIVE,
          date = LocalDate.of(2026, 2, 2),
          reason = NDNameCode("Reason 03", "R03"),
          minutes = 40,
        ),
        NDAdjustment(
          id = 3L,
          reference = UUID(3L, 3L),
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.of(2026, 3, 3),
          reason = NDNameCode("Reason 08", "R08"),
          minutes = 20,
        ),
        NDAdjustment(
          id = 4L,
          reference = UUID(4L, 4L),
          type = NDAdjustmentType.POSITIVE,
          date = LocalDate.of(2026, 4, 4),
          reason = NDNameCode("Reason 02", "R02"),
          minutes = 60,
        ),
        NDAdjustment(
          id = 5L,
          reference = UUID(5L, 5L),
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.of(2026, 5, 5),
          reason = NDNameCode("Reason 07", "R07"),
          minutes = 80,
        ),
        NDAdjustment(
          id = 6L,
          reference = UUID(6L, 6L),
          type = NDAdjustmentType.POSITIVE,
          date = LocalDate.of(2026, 6, 6),
          reason = NDNameCode("Reason 01", "R01"),
          minutes = 70,
        ),
        NDAdjustment(
          id = 7L,
          reference = UUID(7L, 7L),
          type = NDAdjustmentType.NEGATIVE,
          date = LocalDate.of(2026, 7, 7),
          reason = NDNameCode("Reason 05", "R05"),
          minutes = 50,
        ),
        NDAdjustment(
          id = 8L,
          reference = UUID(8L, 8L),
          type = NDAdjustmentType.POSITIVE,
          date = LocalDate.of(2026, 8, 8),
          reason = NDNameCode("Reason 04", "R04"),
          minutes = 10,
        ),
      )

      every { communityPaybackAndDeliusClient.getAdjustments(any(), any()) } returns NDAdjustmentResponse(
        adjustments = adjustments,
      )

      val pageable = PageRequest.of(page, 2, direction, sortField)
      val page = service.getAdjustments("123456", 1, pageable)

      assertThat(page.totalPages).isEqualTo(4)
      assertThat(page.totalElements).isEqualTo(8)
      assertThat(page.content.map { it.id }).isEqualTo(expectedIds)
    }
  }

  @Nested
  inner class ImportAdjustment {
    private val reason = AdjustmentReasonEntity.valid()
    private val adjustment = NDAdjustment.valid().copy(
      reference = null,
      reason = NDNameCode(reason.name, reason.deliusCode),
      type = NDAdjustmentType.NEGATIVE,
    )
    private val reference = UUID.randomUUID()
    private val savedAdjustment = AdjustmentEntity(
      id = reference,
      deliusId = adjustment.id,
      crn = CRN,
      deliusEventNumber = EVENT_NUMBER,
      type = AdjustmentType.NEGATIVE,
      minutes = adjustment.minutes,
      date = adjustment.date,
      reason = reason,
      appointment = null,
    )

    @BeforeEach
    fun setUp() {
      every { adjustmentEntityRepository.findByDeliusId(adjustment.id) } returns null
      every { adjustmentEntityRepository.findByIdOrNull(any()) } returns null
      every { adjustmentEventEntityRepository.findByDeliusAdjustmentIdAndEventType(adjustment.id, AdjustmentEventType.CREATE) } returns null
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))
      every { adjustmentReasonEntityRepository.findByDeliusCode(reason.deliusCode) } returns reason
      every { adjustmentEntityRepository.saveAndFlush(any()) } answers { firstArg<AdjustmentEntity>() }
    }

    @Test
    fun `returns the local adjustment without checking CREATE events or calling Delius`() {
      every { adjustmentEntityRepository.findByDeliusId(adjustment.id) } returns savedAdjustment

      assertThat(importAdjustment()).isEqualTo(savedAdjustment.toDto())

      verify { listOf(adjustmentEventEntityRepository, communityPaybackAndDeliusClient) wasNot Called }
    }

    @Test
    fun `returns null when the local adjustment belongs to another event`() {
      every { adjustmentEntityRepository.findByDeliusId(adjustment.id) } returns savedAdjustment.copy(deliusEventNumber = EVENT_NUMBER + 1)

      assertThat(importAdjustment()).isNull()

      verify { listOf(adjustmentEventEntityRepository, communityPaybackAndDeliusClient) wasNot Called }
    }

    @Test
    fun `links the CREATE event UUID when Delius returns no reference`() {
      val createEvent = stubCreateEvent()

      assertThat(importAdjustment()).isEqualTo(savedAdjustment.toDto())

      verify { adjustmentEntityRepository.saveAndFlush(savedAdjustment.copy(appointment = createEvent.appointment)) }
      verify(exactly = 1) { communityPaybackAndDeliusClient.setAdjustmentReference(adjustment.id, NDAdjustmentReferenceRequest(createEvent.id)) }
    }

    @Test
    fun `uses the CREATE event UUID and appointment when Delius returns the same reference`() {
      val createEvent = stubCreateEvent()
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment.copy(reference = reference)))

      assertThat(importAdjustment()).isEqualTo(savedAdjustment.toDto())

      verify { adjustmentEntityRepository.saveAndFlush(savedAdjustment.copy(appointment = createEvent.appointment)) }
      verify(exactly = 0) { communityPaybackAndDeliusClient.setAdjustmentReference(any(), any()) }
    }

    @Test
    fun `reuses the reference returned by Delius without relinking`() {
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment.copy(reference = reference)))

      assertThat(importAdjustment()).isEqualTo(savedAdjustment.toDto())

      verify { adjustmentEntityRepository.saveAndFlush(savedAdjustment) }
      verify(exactly = 0) { communityPaybackAndDeliusClient.setAdjustmentReference(any(), any()) }
    }

    @Test
    fun `saves a new adjustment before linking its generated UUID`() {
      val imported = requireNotNull(importAdjustment())
      val generatedReference = requireNotNull(imported.id)
      assertThat(imported).isEqualTo(adjustment.copy(reference = generatedReference).toDto())

      verifyOrder {
        adjustmentEntityRepository.saveAndFlush(savedAdjustment.copy(id = generatedReference))
        communityPaybackAndDeliusClient.setAdjustmentReference(adjustment.id, NDAdjustmentReferenceRequest(generatedReference))
      }
    }

    @Test
    fun `publishes no event when linking succeeds`() {
      assertThat(importAdjustment()).isNotNull()

      verify { springEventPublisher wasNot Called }
    }

    @Test
    fun `publishes no event when linking fails`() {
      every { communityPaybackAndDeliusClient.setAdjustmentReference(adjustment.id, any()) } throws WebClientResponseExceptionFactory.conflict()

      assertThatThrownBy { importAdjustment() }.isInstanceOf(ConflictException::class.java)

      verify { springEventPublisher wasNot Called }
    }

    @Test
    fun `returns null when the requested ID is absent from the Delius response`() {
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment.copy(id = adjustment.id + 1)))

      assertThat(importAdjustment()).isNull()

      verify(exactly = 0) { adjustmentEntityRepository.saveAndFlush(any()) }
    }

    @Test
    fun `returns null when Delius returns not found`() {
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } throws WebClientResponseExceptionFactory.notFound()

      assertThat(importAdjustment()).isNull()
    }

    @Test
    fun `rejects an unconfigured adjustment reason`() {
      every { adjustmentReasonEntityRepository.findByDeliusCode(reason.deliusCode) } returns null

      assertThatThrownBy { importAdjustment() }
        .isInstanceOf(BadRequestException::class.java)
        .hasMessage("Unknown Delius adjustment reason '${reason.deliusCode}'")

      verify(exactly = 0) { communityPaybackAndDeliusClient.setAdjustmentReference(any(), any()) }
    }

    @Test
    fun `rejects a Delius reference owned by a different local adjustment`() {
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment.copy(reference = reference)))
      every { adjustmentEntityRepository.findByIdOrNull(reference) } returns savedAdjustment.copy(deliusId = adjustment.id + 1)

      assertThatThrownBy { importAdjustment() }
        .isInstanceOf(ConflictException::class.java)
        .hasMessage("Community Payback reference $reference belongs to a different Delius adjustment")

      verify(exactly = 0) { adjustmentEntityRepository.saveAndFlush(any()) }
    }

    @Test
    fun `rejects a Delius reference that differs from the CREATE event UUID`() {
      stubCreateEvent()
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment.copy(reference = UUID.randomUUID())))

      assertThatThrownBy { importAdjustment() }
        .isInstanceOf(ConflictException::class.java)
        .hasMessage("Delius adjustment ${adjustment.id} already has a different reference")

      verify(exactly = 0) { communityPaybackAndDeliusClient.setAdjustmentReference(any(), any()) }
    }

    private fun stubCreateEvent(): AdjustmentEventEntity {
      val createEvent = AdjustmentEventEntity.valid().copy(
        id = reference,
        deliusAdjustmentId = adjustment.id,
        eventType = AdjustmentEventType.CREATE,
      )
      every { adjustmentEventEntityRepository.findByDeliusAdjustmentIdAndEventType(adjustment.id, AdjustmentEventType.CREATE) } returns createEvent
      return createEvent
    }

    private fun importAdjustment() = service.importAdjustment(UNPAID_WORK_DETAILS, adjustment.id)
  }

  @Nested
  inner class CreateAdjustment {

    @Test
    fun success() {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)
      val appointment = AppointmentEntity.valid()
      val id = UUID.randomUUID()
      val dateOfAdjustment = LocalDate.now().minusDays(3)

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = reason.id,
        type = CreateAdjustmentTypeDto.Positive,
        minutes = 50,
        adjustmentDate = dateOfAdjustment,
      )

      every { adjustmentIdGenerator.generateId(request) } returns id

      every {
        adjustmentValidationService.validate(request, any())
      } answers {
        val ctx = it.invocation.args[1] as AdjustmentValidationContext

        ctx.reason = reason
        ctx.appointment = appointment
        ctx.unpaidWorkDetails = UnpaidWorkDetailsDto.valid()
        ctx.remainingMinutesAllowance = Duration.ofMinutes(100)

        ValidationResult.success()
      }

      every {
        communityPaybackAndDeliusClient.postAdjustments(
          username = "username",
          adjustmentRequests = listOf(
            request.toNDAdjustmentRequest(
              crn = CRN,
              deliusEventNumber = EVENT_NUMBER,
              reason = reason,
              reference = id,
              dateOfAdjustment = dateOfAdjustment,
            ),
          ),
        )
      } returns listOf(NDAdjustmentPostResponse(5L))

      val adjustment = AdjustmentEntity(
        id = id,
        deliusId = 5L,
        crn = CRN,
        deliusEventNumber = EVENT_NUMBER,
        type = AdjustmentType.POSITIVE,
        minutes = 50,
        date = dateOfAdjustment,
        reason = reason,
        appointment = appointment,
      )
      every { adjustmentEntityRepository.save(adjustment) } returns adjustment

      service.createAdjustment(
        upwDetailsId = UNPAID_WORK_DETAILS,
        createAdjustment = request,
        username = USERNAME,
      )

      verifyOrder {
        communityPaybackAndDeliusClient.deleteAdjustment(id)

        communityPaybackAndDeliusClient.postAdjustments(
          username = any(),
          adjustmentRequests = match { it.size == 1 && it.first().reference == id },
        )
        adjustmentEntityRepository.save(adjustment)
        springEventPublisher.publishEvent(
          AdjustmentCreatedEvent(
            adjustmentEntity = adjustment,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = appointment.id.toString(),
            ),
          ),
        )
      }
    }
  }

  @Nested
  inner class CreateAdjustmentWithTriggerType {

    @Test
    fun `automated adjustments use fresh references and record the provided trigger type`() {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 180)
      val appointment = AppointmentEntity.valid()
      val request = CreateAdjustmentDto.valid().copy(
        taskId = null,
        adjustmentReasonId = reason.id,
        appointmentId = appointment.id,
        type = CreateAdjustmentTypeDto.Positive,
        minutes = 60,
        adjustmentDate = LocalDate.now().minusDays(1),
      )

      every { adjustmentValidationService.validate(request, any()) } answers {
        val ctx = it.invocation.args[1] as AdjustmentValidationContext
        ctx.reason = reason
        ctx.appointment = appointment
        ctx.unpaidWorkDetails = UnpaidWorkDetailsDto.valid()
        ctx.remainingMinutesAllowance = Duration.ofMinutes(600)
        ValidationResult.success()
      }
      every { communityPaybackAndDeliusClient.postAdjustments(USERNAME, any()) } returns listOf(NDAdjustmentPostResponse(5L))
      val saved = mutableListOf<AdjustmentEntity>()
      every { adjustmentEntityRepository.save(capture(saved)) } returnsArgument 0

      repeat(2) {
        service.createAdjustment(
          upwDetailsId = UNPAID_WORK_DETAILS,
          createAdjustment = request,
          username = USERNAME,
          adjustmentEventTriggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
        )
      }

      assertThat(saved).hasSize(2)
      assertThat(saved.map { it.id }).doesNotHaveDuplicates()
      saved.forEach { adjustment ->
        verify(exactly = 1) {
          communityPaybackAndDeliusClient.postAdjustments(
            USERNAME,
            listOf(request.toNDAdjustmentRequest(CRN, EVENT_NUMBER, reason, adjustment.id, request.adjustmentDate!!)),
          )
          springEventPublisher.publishEvent(
            AdjustmentCreatedEvent(
              adjustmentEntity = adjustment,
              trigger = AdjustmentEventTrigger(
                triggeredAt = OffsetDateTime.now(clock),
                triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
                triggeredBy = appointment.id.toString(),
              ),
            ),
          )
        }
      }
      verify { adjustmentIdGenerator wasNot Called }
    }
  }

  @Nested
  inner class DeleteAdjustment {

    @BeforeEach
    fun setUp() {
      every { adjustmentEntityRepository.findByIdOrNull(any()) } returns null
    }

    @Test
    fun success() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.Success)

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify {
        springEventPublisher.publishEvent(
          AdjustmentDeletedEvent(
            id = expectedId,
            eventToDelete = expectedEvent,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = USERNAME,
            ),
          ),
        )
      }
    }

    @Test
    fun `records the provided trigger type on the adjustment deleted event`() {
      val adjustmentId = UUID.randomUUID()
      val createEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns createEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      val result = service.deleteAdjustment(adjustmentId, USERNAME, AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.Success)
      verify {
        springEventPublisher.publishEvent(
          AdjustmentDeletedEvent(
            id = expectedId,
            eventToDelete = createEvent,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.AUTOMATED_TRAVEL_TIME,
              triggeredBy = USERNAME,
            ),
          ),
        )
      }
    }

    @Test
    fun `deletes a stored adjustment without a CREATE event`() {
      val adjustment = AdjustmentEntity(
        id = UUID.randomUUID(),
        deliusId = 123L,
        crn = CRN,
        deliusEventNumber = EVENT_NUMBER,
        type = AdjustmentType.POSITIVE,
        minutes = 30,
        date = LocalDate.now(clock),
        reason = AdjustmentReasonEntity.valid(),
        appointment = null,
      )
      val eventId = UUID.randomUUID()
      every { adjustmentEntityRepository.findByIdOrNull(adjustment.id) } returns adjustment
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustment.id) } returns null
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustment.id)) } returns eventId

      assertThat(service.deleteAdjustment(adjustment.id, USERNAME)).isEqualTo(DeleteAdjustmentResult.Success)

      verifyOrder {
        communityPaybackAndDeliusClient.deleteAdjustment(adjustment.id)
        springEventPublisher.publishEvent(
          AdjustmentDeletedEvent(
            id = eventId,
            eventToDelete = null,
            adjustment = adjustment,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = USERNAME,
            ),
          ),
        )
      }
    }

    @Test
    fun `returns not found if no entity with correct ID is in repository`() {
      val adjustmentId = UUID.randomUUID()
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns null

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.NotFound)

      verify(exactly = 0) {
        communityPaybackAndDeliusClient.deleteAdjustment(any())

        springEventPublisher.publishEvent(any())
      }
    }

    @Test
    fun `returns not found if upstream client returns 404`() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      every { communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId) } throws WebClientResponseExceptionFactory.notFound()

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isEqualTo(DeleteAdjustmentResult.NotFound)

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify(exactly = 0) {
        springEventPublisher.publishEvent(any())
      }
    }

    @Test
    fun `returns failure if upstream client returns error response`() {
      val adjustmentId = UUID.randomUUID()
      val expectedEvent = AdjustmentEventEntity.valid().copy(id = adjustmentId, eventType = AdjustmentEventType.CREATE)
      every { adjustmentEventEntityRepository.findByIdOrNull(adjustmentId) } returns expectedEvent

      val expectedId = UUID.randomUUID()
      every { adjustmentIdGenerator.generateId(DeleteAdjustmentProperties(adjustmentId)) } returns expectedId

      every { communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId) } throws WebClientResponseExceptionFactory.badRequest("Some error")

      val result = service.deleteAdjustment(adjustmentId, USERNAME)

      assertThat(result).isInstanceOf(DeleteAdjustmentResult.Failed::class.java)
      assertThat((result as DeleteAdjustmentResult.Failed).exception.statusCode.value()).isEqualTo(400)
      assertThat(result.exception.responseBodyAsString).isEqualTo("Some error")

      verifyOrder {
        adjustmentEventEntityRepository.findByIdOrNull(adjustmentId)

        communityPaybackAndDeliusClient.deleteAdjustment(adjustmentId)
      }

      verify(exactly = 0) {
        springEventPublisher.publishEvent(any())
      }
    }
  }
}

class PageableArgumentsProvider : ArgumentsProvider {
  override fun provideArguments(
    parameters: ParameterDeclarations,
    context: ExtensionContext,
  ): Stream<Arguments> = Stream.of(
    Arguments.arguments("type", Sort.Direction.ASC, 0, listOf(UUID(2L, 2L), UUID(4L, 4L))),
    Arguments.arguments("date", Sort.Direction.DESC, 1, listOf(UUID(6L, 6L), UUID(5L, 5L))),
    Arguments.arguments("reason", Sort.Direction.ASC, 2, listOf(UUID(7L, 7L), UUID(1L, 1L))),
    Arguments.arguments("minutes", Sort.Direction.DESC, 3, listOf(UUID(3L, 3L), UUID(8L, 8L))),
  )
}

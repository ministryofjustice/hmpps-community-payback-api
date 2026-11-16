package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

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
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.support.ParameterDeclarations
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentPostResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAdjustmentType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDNameCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDUpdateAdjustmentReference
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentFilterTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateAdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UnpaidWorkDetailsIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventTriggerType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentEventType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AdjustmentReasonEntityRepository
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toEntity
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
  lateinit var adjustmentEventEntityRepository: AdjustmentEventEntityRepository

  @RelaxedMockK
  lateinit var adjustmentEntityRepository: AdjustmentEntityRepository

  @RelaxedMockK
  lateinit var adjustmentReasonEntityRepository: AdjustmentReasonEntityRepository

  val clock: Clock = ClockConfiguration.MutableClock(Instant.now())

  @InjectMockKs
  private lateinit var service: AdjustmentService

  @BeforeEach
  fun setupAdjustmentRepository() {
    every { adjustmentReasonEntityRepository.findByDeliusCodeIn(any()) } returns emptyList()
    every { adjustmentEntityRepository.findAllById(any()) } returns emptyList()
    every { adjustmentEntityRepository.findByDeliusAdjustmentIdIn(any()) } returns emptyList()
    every { adjustmentEntityRepository.saveAll(any<List<AdjustmentEntity>>()) } answers { firstArg() }
    every { adjustmentEventEntityRepository.findAllById(any()) } returns emptyList()
  }

  private fun stubAdjustmentReasons(adjustments: List<NDAdjustment>) {
    val reasons = adjustments.map { it.reason }.distinctBy { it.code }.map {
      AdjustmentReasonEntity.valid().copy(name = it.name, deliusCode = it.code)
    }
    every { adjustmentReasonEntityRepository.findByDeliusCodeIn(reasons.map { it.deliusCode }) } returns reasons
  }

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
    fun `empty retrieval does not access persistence or update references`() {
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(emptyList())

      assertThat(service.getAdjustments(CRN, EVENT_NUMBER)).isEmpty()

      verify(exactly = 0) {
        adjustmentEntityRepository.saveAll(any<List<AdjustmentEntity>>())
        adjustmentEntityRepository.findByDeliusAdjustmentIdIn(any())
        adjustmentEntityRepository.findAllById(any())
        adjustmentReasonEntityRepository.findByDeliusCodeIn(any())
        adjustmentEventEntityRepository.findAllById(any())
        communityPaybackAndDeliusClient.updateAdjustmentReference(any(), any())
      }
    }

    @Test
    fun `saves adjustments in one batch and returns the original Delius order`() {
      val adjustments = List(4) { NDAdjustment.valid() }
      val appointments = List(2) { AppointmentEntity.valid() }
      val events = adjustments.take(2).mapIndexed { index, adjustment ->
        AdjustmentEventEntity.valid().copy(
          id = adjustment.reference!!,
          eventType = AdjustmentEventType.CREATE,
          appointment = appointments[index],
        )
      }
      stubAdjustmentReasons(adjustments)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(adjustments)
      every { adjustmentEventEntityRepository.findAllById(adjustments.mapNotNull { it.reference }) } returns events.reversed()

      val result = service.getAdjustments(CRN, EVENT_NUMBER)

      assertThat(result).containsExactlyElementsOf(adjustments.map { it.toDto() })
      verify(exactly = 1) {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { batch ->
            batch.map { it.deliusAdjustmentId } == adjustments.map { it.id } && batch.map { it.appointment } == appointments + listOf(null, null)
          },
        )
        adjustmentEventEntityRepository.findAllById(adjustments.mapNotNull { it.reference })
      }
    }

    @Test
    fun `a batch persistence failure prevents reference updates`() {
      val adjustments = List(2) { NDAdjustment.valid().copy(reference = null) }
      val error = IllegalStateException("Batch could not be saved")
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(adjustments)
      every { adjustmentEntityRepository.saveAll(any<List<AdjustmentEntity>>()) } throws error

      assertThatThrownBy { service.getAdjustments(CRN, EVENT_NUMBER) }.isSameAs(error)

      verify(exactly = 0) { communityPaybackAndDeliusClient.updateAdjustmentReference(any(), any()) }
    }

    @Test
    fun `sets the new local reference in Delius when the adjustment has no reference`() {
      val adjustment = NDAdjustment.valid().copy(reference = null)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))

      val result = service.getAdjustments(CRN, EVENT_NUMBER).single()

      assertThat(result.id).isNotNull()
      verify {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            entities.any { it.id == result.id && it.deliusAdjustmentId == adjustment.id }
          },
        )
      }
      verify(exactly = 1) {
        communityPaybackAndDeliusClient.updateAdjustmentReference(adjustment.id, NDUpdateAdjustmentReference(result.id!!))
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `uses the Delius reference or generates one without looking up stored references`(hasReference: Boolean) {
      val original = NDAdjustment.valid()
      val existing = original.toEntity(CRN, EVENT_NUMBER, null)
      val adjustment = original.copy(reference = UUID.randomUUID().takeIf { hasReference })
      every { adjustmentEntityRepository.findByDeliusAdjustmentIdIn(listOf(adjustment.id)) } returns listOf(existing)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))

      val result = service.getAdjustments(CRN, EVENT_NUMBER).single()

      assertThat(result.id).isNotNull().isNotEqualTo(existing.id)
      if (hasReference) {
        assertThat(result.id).isEqualTo(adjustment.reference)
      }
      verify(exactly = if (hasReference) 0 else 1) {
        communityPaybackAndDeliusClient.updateAdjustmentReference(adjustment.id, NDUpdateAdjustmentReference(result.id!!))
      }
      verify(exactly = 0) {
        adjustmentEntityRepository.findByDeliusAdjustmentIdIn(any())
        adjustmentEntityRepository.findAllById(any())
      }
      verify(exactly = 1) {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            entities.size == 1 && entities.single().id == result.id && entities.single().deliusAdjustmentId == adjustment.id
          },
        )
      }
    }

    @Test
    fun `propagates a failure to update the Delius reference`() {
      val adjustment = NDAdjustment.valid().copy(reference = null)
      val error = WebClientResponseExceptionFactory.badRequest("Could not update adjustment reference")
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))
      every { communityPaybackAndDeliusClient.updateAdjustmentReference(adjustment.id, any()) } throws error

      assertThatThrownBy { service.getAdjustments(CRN, EVENT_NUMBER) }.isSameAs(error)
    }

    @ParameterizedTest
    @CsvSource("CREATE,true", "CREATE,false", "DELETE,true", "DELETE,false")
    fun `retrieval links appointments only from matching creation events`(eventType: AdjustmentEventType, hasAppointment: Boolean) {
      val adjustment = NDAdjustment.valid().copy(id = 100L)
      val event = AdjustmentEventEntity.valid().copy(
        id = adjustment.reference!!,
        deliusAdjustmentId = 99L,
        eventType = eventType,
        appointment = AppointmentEntity.valid().takeIf { hasAppointment },
      )
      val expectedAppointment = event.appointment.takeIf { eventType == AdjustmentEventType.CREATE }
      every { adjustmentEventEntityRepository.findAllById(listOf(event.id)) } returns listOf(event)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))

      service.getAdjustments(CRN, EVENT_NUMBER)

      verify(exactly = 1) {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            val saved = entities.singleOrNull()
            saved != null && saved.appointment == expectedAppointment && saved.deliusAdjustmentId == adjustment.id && saved.id == adjustment.reference
          },
        )
        adjustmentEventEntityRepository.findAllById(listOf(event.id))
      }
    }

    @Test
    fun `saves refreshed adjustments without loading or mutating existing entities`() {
      val appointment = AppointmentEntity.valid()
      val adjustment = NDAdjustment.valid()
      val existing = adjustment.toEntity(CRN, EVENT_NUMBER, null).apply { this.appointment = appointment }
      val updated = adjustment.copy(minutes = adjustment.minutes + 5)
      every { adjustmentEntityRepository.findByDeliusAdjustmentIdIn(listOf(adjustment.id)) } returns listOf(existing)
      every { adjustmentEntityRepository.findAllById(listOf(adjustment.reference!!)) } returns listOf(existing)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(updated))

      service.getAdjustments(CRN, EVENT_NUMBER)

      assertThat(existing.appointment).isSameAs(appointment)
      assertThat(existing.minutes).isEqualTo(adjustment.minutes)
      verify(exactly = 1) {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            val saved = entities.singleOrNull()
            saved != null && saved.id == updated.reference && saved.minutes == updated.minutes && saved.appointment == null
          },
        )
      }
      verify(exactly = 0) {
        adjustmentEntityRepository.findByDeliusAdjustmentIdIn(any())
        adjustmentEntityRepository.findAllById(any())
      }
    }

    @Test
    fun `links adjustments to their matching local reason`() {
      val adjustment = NDAdjustment.valid()
      val reason = AdjustmentReasonEntity.valid().copy(deliusCode = adjustment.reason.code)
      every { adjustmentReasonEntityRepository.findByDeliusCodeIn(listOf(adjustment.reason.code)) } returns listOf(reason)
      every { communityPaybackAndDeliusClient.getAdjustments(CRN, EVENT_NUMBER) } returns NDAdjustmentResponse(listOf(adjustment))

      val result = service.getAdjustments(CRN, EVENT_NUMBER).single()

      assertThat(result.reason).isEqualTo(reason.name)
      assertThat(result.reasonCode).isEqualTo(reason.deliusCode)

      verify {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            entities.any { it.adjustmentReason == reason }
          },
        )
      }
    }

    @Test
    fun `paginates other adjustments whose local reason name differs from TTX`() {
      val adjustments = listOf(
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 8, 8), reason = NDNameCode("Travel Time", "TTX")),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 7, 7), reason = NDNameCode("Other", "OTH")),
        NDAdjustment.valid().copy(date = LocalDate.of(2026, 6, 6), reason = NDNameCode("Other", "OTH")),
      )
      stubAdjustmentReasons(adjustments)
      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(adjustments)

      val result = service.getAdjustments("X123456", 1, PageRequest.of(1, 1, Sort.by("date").descending()), AdjustmentFilterTypeDto.OTHER)

      assertThat(result.content).containsExactly(adjustments[2].toDto())
      assertThat(result.totalElements).isEqualTo(2)
      assertThat(result.totalPages).isEqualTo(2)
    }

    @Test
    fun `filters travel time by local reason name and returns an empty page when no adjustments match`() {
      val travelTime = NDAdjustment.valid().copy(reason = NDNameCode("TTX", "TTX"))
      val other = NDAdjustment.valid().copy(reason = NDNameCode("Other", "OTH"))
      stubAdjustmentReasons(listOf(travelTime, other))
      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(listOf(travelTime, other))
      val pageable = PageRequest.of(0, 10, Sort.by("date"))

      val result = service.getAdjustments("X123456", 1, pageable, AdjustmentFilterTypeDto.TRAVEL_TIME)

      assertThat(result.content).containsExactly(travelTime.toDto())
      assertThat(result.totalElements).isEqualTo(1)

      every { communityPaybackAndDeliusClient.getAdjustments("X123456", 1) } returns NDAdjustmentResponse(listOf(travelTime))
      stubAdjustmentReasons(listOf(travelTime))
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
      stubAdjustmentReasons(adjustments)

      every { communityPaybackAndDeliusClient.getAdjustments(any(), any()) } returns NDAdjustmentResponse(
        adjustments = adjustments,
      )

      val results = service.getAdjustments("X123456", 1)

      assertThat(results).hasSameElementsAs(adjustments.map { it.toDto() })
      verify(exactly = 0) { communityPaybackAndDeliusClient.updateAdjustmentReference(any(), any()) }
      verify(exactly = 1) { adjustmentReasonEntityRepository.findByDeliusCodeIn(adjustments.map { it.reason.code }.distinct()) }
      adjustments.forEach { adjustment ->
        verify {
          adjustmentEntityRepository.saveAll(
            match<List<AdjustmentEntity>> { entities ->
              entities.any { it.id == adjustment.reference && it.deliusAdjustmentId == adjustment.id && it.crn == "X123456" && it.deliusEventNumber == 1 }
            },
          )
        }
      }
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
      stubAdjustmentReasons(adjustments)

      every { communityPaybackAndDeliusClient.getAdjustments(any(), any()) } returns NDAdjustmentResponse(
        adjustments = adjustments,
      )

      val pageable = PageRequest.of(page, 2, direction, sortField)
      val page = service.getAdjustments("123456", 1, pageable)

      verify(exactly = 1) { adjustmentReasonEntityRepository.findByDeliusCodeIn(adjustments.map { it.reason.code }) }
      adjustments.forEach { adjustment ->
        verify {
          adjustmentEntityRepository.saveAll(
            match<List<AdjustmentEntity>> { entities ->
              entities.any { it.id == adjustment.reference && it.deliusAdjustmentId == adjustment.id }
            },
          )
        }
      }

      assertThat(page.totalPages).isEqualTo(4)
      assertThat(page.totalElements).isEqualTo(8)
      assertThat(page.content.map { it.id }).isEqualTo(expectedIds)
    }
  }

  @Nested
  inner class CreateAdjustment {

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun success(linkToAppointment: Boolean) {
      val reason = AdjustmentReasonEntity.valid().copy(maxMinutesAllowed = 50)
      val appointment = AppointmentEntity.valid().takeIf { linkToAppointment }
      val id = UUID.randomUUID()
      val dateOfAdjustment = LocalDate.now().minusDays(3)

      val request = CreateAdjustmentDto.valid().copy(
        adjustmentReasonId = reason.id,
        minutes = 50,
        adjustmentDate = dateOfAdjustment,
        appointmentId = appointment?.id,
      )

      every { adjustmentIdGenerator.generateId(request) } returns id
      every { adjustmentReasonEntityRepository.findByDeliusCodeIn(listOf(reason.deliusCode)) } returns listOf(reason)
      if (linkToAppointment) {
        val conflictingEvent = AdjustmentEventEntity.valid().copy(id = id, eventType = AdjustmentEventType.CREATE, appointment = AppointmentEntity.valid())
        every { adjustmentEventEntityRepository.findAllById(listOf(id)) } returns listOf(conflictingEvent)
      }

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

      val result = service.createAdjustment(
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
      }

      verify {
        springEventPublisher.publishEvent(
          AdjustmentCreatedEvent(
            id = id,
            createDto = request,
            appointmentEntity = appointment,
            reason = reason,
            deliusAdjustmentId = 5L,
            trigger = AdjustmentEventTrigger(
              triggeredAt = OffsetDateTime.now(clock),
              triggerType = AdjustmentEventTriggerType.APPOINTMENT_TASK,
              triggeredBy = appointment?.id.toString(),
            ),
            adjustmentDate = dateOfAdjustment,
          ),
        )
      }
      verify {
        adjustmentEntityRepository.saveAll(
          match<List<AdjustmentEntity>> { entities ->
            entities.any {
              it.id == id &&
                it.deliusAdjustmentId == 5L &&
                it.crn == CRN &&
                it.deliusEventNumber == EVENT_NUMBER &&
                it.adjustmentDate == dateOfAdjustment &&
                it.minutes == request.minutes &&
                it.adjustmentReason == reason &&
                it.appointment == appointment
            }
          },
        )
      }
      assertThat(result.id).isEqualTo(id)
      assertThat(result.deliusId).isEqualTo(5L)
      assertThat(result.date).isEqualTo(dateOfAdjustment)
      assertThat(result.reason).isEqualTo(reason.name)
      assertThat(result.reasonCode).isEqualTo(reason.deliusCode)
      verify(exactly = 0) {
        communityPaybackAndDeliusClient.getAdjustment(any())
        communityPaybackAndDeliusClient.updateAdjustmentReference(any(), any())
      }
    }
  }

  @Nested
  inner class DeleteAdjustment {

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
    Arguments.arguments("minutes", Sort.Direction.ASC, 0, listOf(UUID(5L, 5L), UUID(7L, 7L))),
    Arguments.arguments("date", Sort.Direction.DESC, 1, listOf(UUID(6L, 6L), UUID(5L, 5L))),
    Arguments.arguments("reason", Sort.Direction.ASC, 2, listOf(UUID(7L, 7L), UUID(1L, 1L))),
    Arguments.arguments("minutes", Sort.Direction.DESC, 3, listOf(UUID(7L, 7L), UUID(5L, 5L))),
  )
}

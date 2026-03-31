package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validCreateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.validUpdateAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.validPending
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.event.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentEventTrigger
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentTaskService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentValidationService.ValidatedAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.CreateAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.CommunityPaybackSpringEvent.UpdateAppointmentEvent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AppointmentTaskServiceTest {

  @RelaxedMockK
  private lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @RelaxedMockK
  private lateinit var appointmentRetrievalService: AppointmentRetrievalService

  @RelaxedMockK
  private lateinit var contextService: ContextService

  @InjectMockKs
  private lateinit var service: AppointmentTaskService

  private companion object {
    const val PROVIDER_CODE = "PROV123"
  }

  @Nested
  inner class CreateTravelTimeTaskOnAppointmentCreation {

    @Test
    fun `no outcome, do nothing`() {
      val event = AppointmentCreatedEvent.valid().copy(
        createDto = ValidatedAppointment.validCreateAppointment().copy(
          contactOutcome = null,
        ),
      )

      service.createTravelTimeTaskOnAppointmentCreation(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @Test
    fun `outcome not attended, do nothing`() {
      val event = AppointmentCreatedEvent.valid().copy(
        createDto = ValidatedAppointment.validCreateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = false),
        ),
      )

      service.createTravelTimeTaskOnAppointmentCreation(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @ParameterizedTest
    @CsvSource("ETE", "INDUCTION")
    fun `project group time doesn't support travel time, do nothing`(
      projectTypeGroup: ProjectTypeGroupDto,
    ) {
      val event = AppointmentCreatedEvent.valid().copy(
        createDto = ValidatedAppointment.validCreateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = true),
          project = ProjectDto.valid().copy(projectType = ProjectTypeDto.valid().copy(group = projectTypeGroup)),
        ),
      )

      service.createTravelTimeTaskOnAppointmentCreation(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @ParameterizedTest
    @CsvSource("GROUP", "INDIVIDUAL")
    fun `only create task if outcome is attended and project group type supports travel time`(
      projectTypeGroup: ProjectTypeGroupDto,
    ) {
      val event = AppointmentCreatedEvent.valid().copy(
        createDto = ValidatedAppointment.validCreateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = true),
          project = ProjectDto.valid().copy(projectType = ProjectTypeDto.valid().copy(group = projectTypeGroup)),
        ),
      )

      val taskSlot = slot<AppointmentTaskEntity>()
      every { appointmentTaskEntityRepository.save(capture(taskSlot)) } returnsArgument 0

      service.createTravelTimeTaskOnAppointmentCreation(event)

      assertThat(taskSlot.isCaptured).isTrue
      assertThat(taskSlot.captured.appointment).isEqualTo(event.appointmentEntity)
      assertThat(taskSlot.captured.taskType).isEqualTo(AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME)
      assertThat(taskSlot.captured.taskStatus).isEqualTo(AppointmentTaskStatus.PENDING)
    }
  }

  @Nested
  inner class CreateTravelTimeTaskOnAppointmentUpdate {

    @Test
    fun `no outcome, do nothing`() {
      val event = UpdateAppointmentEvent.valid().copy(
        updateDto = ValidatedAppointment.validUpdateAppointment().copy(
          contactOutcome = null,
        ),
      )

      service.createTravelTimeTaskOnAppointmentUpdate(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @Test
    fun `outcome not attended, do nothing`() {
      val event = UpdateAppointmentEvent.valid().copy(
        updateDto = ValidatedAppointment.validUpdateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = false),
        ),
      )

      service.createTravelTimeTaskOnAppointmentUpdate(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @ParameterizedTest
    @CsvSource("ETE", "INDUCTION")
    fun `project group time doesn't support travel time, do nothing`(
      projectTypeGroup: ProjectTypeGroupDto,
    ) {
      val event = UpdateAppointmentEvent.valid().copy(
        updateDto = ValidatedAppointment.validUpdateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = true),
          project = ProjectDto.valid().copy(projectType = ProjectTypeDto.valid().copy(group = projectTypeGroup)),
        ),
      )

      service.createTravelTimeTaskOnAppointmentUpdate(event)

      verify(exactly = 0) { appointmentTaskEntityRepository.save(any()) }
    }

    @ParameterizedTest
    @CsvSource("GROUP", "INDIVIDUAL")
    fun `only create task if outcome is attended and project group type supports travel time`(
      projectTypeGroup: ProjectTypeGroupDto,
    ) {
      val event = UpdateAppointmentEvent.valid().copy(
        updateDto = ValidatedAppointment.validUpdateAppointment().copy(
          contactOutcome = ContactOutcomeEntity.valid().copy(attended = true),
          project = ProjectDto.valid().copy(projectType = ProjectTypeDto.valid().copy(group = projectTypeGroup)),
        ),
      )

      val taskSlot = slot<AppointmentTaskEntity>()
      every { appointmentTaskEntityRepository.save(capture(taskSlot)) } returnsArgument 0

      service.createTravelTimeTaskOnAppointmentUpdate(event)

      assertThat(taskSlot.isCaptured).isTrue
      assertThat(taskSlot.captured.appointment).isEqualTo(event.appointmentEntity)
      assertThat(taskSlot.captured.taskType).isEqualTo(AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME)
      assertThat(taskSlot.captured.taskStatus).isEqualTo(AppointmentTaskStatus.PENDING)
    }
  }

  @Nested
  inner class CompleteTravelTimeTaskOnAdjustmentCreation {

    @Test
    fun `complete task linked to the adjustment`() {
      val task = AppointmentTaskEntity.validPending()
      val triggeredAt = OffsetDateTime.now()

      every { appointmentTaskEntityRepository.findByIdOrNull(task.id) } returns task
      every { contextService.getUserName() } returns "currentUsername"
      every { appointmentTaskEntityRepository.save(any()) } returnsArgument 0

      service.closeTravelTimeTaskOnAdjustmentCreation(
        CreateAdjustmentEvent.valid().copy(
          trigger = AdjustmentEventTrigger.valid().copy(
            triggeredAt = triggeredAt,
            triggeredBy = task.id.toString(),
          ),
        ),
      )

      verify {
        appointmentTaskEntityRepository.save(task)

        assertThat(task.taskStatus).isEqualTo(AppointmentTaskStatus.COMPLETE)
        assertThat(task.decisionMadeAt).isEqualTo(triggeredAt)
        assertThat(task.decisionMadeByUsername).isEqualTo("currentUsername")
      }
    }
  }

  @Nested
  inner class GetPendingAppointmentTasks {

    @Test
    fun `returns paginated appointment task summaries without filters`() {
      val pageable = PageRequest.of(0, 10)
      val taskId = UUID.randomUUID()
      val appointmentId = UUID.randomUUID()
      val deliusAppointmentId = 101L

      val appointmentEntity = AppointmentEntity.valid().copy(
        id = appointmentId,
        deliusId = deliusAppointmentId,
        providerCode = PROVIDER_CODE,
        date = LocalDate.of(2026, 3, 27),
      )

      val taskEntity = AppointmentTaskEntity(
        id = taskId,
        appointment = appointmentEntity,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        createdAt = OffsetDateTime.now(),
        taskStatus = AppointmentTaskStatus.PENDING,
      )

      val appointmentSummary = AppointmentSummaryDto.valid().copy(id = deliusAppointmentId)

      every {
        appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
          fromDate = null,
          toDate = null,
          providerCode = null,
          pageable = pageable,
        )
      } returns PageImpl(listOf(taskEntity), pageable, 1L)

      every {
        appointmentRetrievalService.getAppointments(
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeGroup = null,
          eventNumber = null,
          appointmentIds = listOf(deliusAppointmentId),
          pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "name")),
        )
      } returns PageImpl(listOf(appointmentSummary), PageRequest.of(0, 1), 1L)

      val result = service.getPendingAppointmentTasks(pageable = pageable)

      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].taskId).isEqualTo(taskId)
      assertThat(result.content[0].appointment).isEqualTo(appointmentSummary)
      assertThat(result.totalElements).isEqualTo(1L)
    }

    @Test
    fun `returns paginated appointment task summaries with all filters`() {
      val pageable = PageRequest.of(0, 5)
      val fromDate = LocalDate.of(2026, 1, 1)
      val toDate = LocalDate.of(2026, 12, 31)
      val providerCode = "PROV456"

      val taskId = UUID.randomUUID()
      val appointmentId = UUID.randomUUID()
      val deliusAppointmentId = 202L

      val appointmentEntity = AppointmentEntity.valid().copy(
        id = appointmentId,
        deliusId = deliusAppointmentId,
        providerCode = providerCode,
        date = LocalDate.of(2026, 6, 15),
      )

      val taskEntity = AppointmentTaskEntity(
        id = taskId,
        appointment = appointmentEntity,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        createdAt = OffsetDateTime.now(),
        taskStatus = AppointmentTaskStatus.PENDING,
      )

      val appointmentSummary = AppointmentSummaryDto.valid().copy(id = deliusAppointmentId)

      every {
        appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
          fromDate = fromDate,
          toDate = toDate,
          providerCode = providerCode,
          pageable = pageable,
        )
      } returns PageImpl(listOf(taskEntity), pageable, 1L)

      every {
        appointmentRetrievalService.getAppointments(
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeGroup = null,
          eventNumber = null,
          appointmentIds = listOf(deliusAppointmentId),
          pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "name")),
        )
      } returns PageImpl(listOf(appointmentSummary), PageRequest.of(0, 1), 1L)

      val result = service.getPendingAppointmentTasks(
        fromDate = fromDate,
        toDate = toDate,
        providerCode = providerCode,
        pageable = pageable,
      )

      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].taskId).isEqualTo(taskId)
      assertThat(result.content[0].appointment).isEqualTo(appointmentSummary)
      assertThat(result.totalElements).isEqualTo(1L)
    }

    @Test
    fun `returns empty page when no pending tasks found`() {
      val pageable = PageRequest.of(0, 10)

      every {
        appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
          fromDate = null,
          toDate = null,
          providerCode = null,
          pageable = pageable,
        )
      } returns PageImpl(emptyList(), pageable, 0L)

      val result = service.getPendingAppointmentTasks(pageable = pageable)

      assertThat(result.content).isEmpty()
      assertThat(result.totalElements).isEqualTo(0L)
    }

    @Test
    fun `returns multiple appointment task summaries`() {
      val pageable = PageRequest.of(0, 20)

      val task1Id = UUID.randomUUID()
      val appointment1Id = UUID.randomUUID()
      val deliusAppointment1Id = 101L

      val task2Id = UUID.randomUUID()
      val appointment2Id = UUID.randomUUID()
      val deliusAppointment2Id = 102L

      val appointmentEntity1 = AppointmentEntity.valid().copy(
        id = appointment1Id,
        deliusId = deliusAppointment1Id,
        providerCode = PROVIDER_CODE,
        date = LocalDate.of(2026, 3, 27),
      )

      val appointmentEntity2 = AppointmentEntity.valid().copy(
        id = appointment2Id,
        deliusId = deliusAppointment2Id,
        providerCode = PROVIDER_CODE,
        date = LocalDate.of(2026, 3, 28),
      )

      val taskEntity1 = AppointmentTaskEntity(
        id = task1Id,
        appointment = appointmentEntity1,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        createdAt = OffsetDateTime.now(),
        taskStatus = AppointmentTaskStatus.PENDING,
      )

      val taskEntity2 = AppointmentTaskEntity(
        id = task2Id,
        appointment = appointmentEntity2,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        createdAt = OffsetDateTime.now(),
        taskStatus = AppointmentTaskStatus.PENDING,
      )

      val appointmentSummary1 = AppointmentSummaryDto.valid().copy(id = deliusAppointment1Id)
      val appointmentSummary2 = AppointmentSummaryDto.valid().copy(id = deliusAppointment2Id)

      every {
        appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
          fromDate = null,
          toDate = null,
          providerCode = null,
          pageable = pageable,
        )
      } returns PageImpl(
        listOf(
          taskEntity1,
          taskEntity2,
        ),
        pageable,
        2L,
      )

      every {
        appointmentRetrievalService.getAppointments(
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeGroup = null,
          eventNumber = null,
          appointmentIds = any(),
          pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "name")),
        )
      } returns PageImpl(listOf(appointmentSummary1, appointmentSummary2), PageRequest.of(0, 2), 2L)

      val result = service.getPendingAppointmentTasks(pageable = pageable)

      assertThat(result.content).hasSize(2)
      assertThat(result.content[0].taskId).isEqualTo(task1Id)
      assertThat(result.content[0].appointment).isEqualTo(appointmentSummary1)
      assertThat(result.content[1].taskId).isEqualTo(task2Id)
      assertThat(result.content[1].appointment).isEqualTo(appointmentSummary2)
      assertThat(result.totalElements).isEqualTo(2L)
    }

    @Test
    fun `filters by fromDate only`() {
      val pageable = PageRequest.of(0, 10)
      val fromDate = LocalDate.of(2026, 3, 1)

      val taskId = UUID.randomUUID()
      val appointmentId = UUID.randomUUID()
      val deliusAppointmentId = 303L

      val appointmentEntity = AppointmentEntity.valid().copy(
        id = appointmentId,
        deliusId = deliusAppointmentId,
        providerCode = PROVIDER_CODE,
        date = LocalDate.of(2026, 3, 27),
      )

      val taskEntity = AppointmentTaskEntity(
        id = taskId,
        appointment = appointmentEntity,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        createdAt = OffsetDateTime.now(),
        taskStatus = AppointmentTaskStatus.PENDING,
      )

      val appointmentSummary = AppointmentSummaryDto.valid().copy(id = deliusAppointmentId)

      every {
        appointmentTaskEntityRepository.findPendingTasksWithFiltersAndAppointments(
          fromDate = fromDate,
          toDate = null,
          providerCode = null,
          pageable = pageable,
        )
      } returns PageImpl(listOf(taskEntity), pageable, 1L)

      every {
        appointmentRetrievalService.getAppointments(
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeGroup = null,
          eventNumber = null,
          appointmentIds = listOf(deliusAppointmentId),
          pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "name")),
        )
      } returns PageImpl(listOf(appointmentSummary), PageRequest.of(0, 1), 1L)

      val result = service.getPendingAppointmentTasks(fromDate = fromDate, pageable = pageable)

      assertThat(result.content).hasSize(1)
      assertThat(result.content[0].taskId).isEqualTo(taskId)
    }
  }
}

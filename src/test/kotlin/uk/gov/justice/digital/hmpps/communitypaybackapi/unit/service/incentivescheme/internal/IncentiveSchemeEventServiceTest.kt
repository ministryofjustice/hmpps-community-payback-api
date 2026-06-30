package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.incentivescheme.internal

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AdjustmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAdjustmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeAppointmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEvent.IncentiveSchemeCourseCompletionAppointmentEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.incentivescheme.internal.IncentiveSchemeEventService
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
class IncentiveSchemeEventServiceTest {
  @MockK
  private lateinit var appointmentService: AppointmentService

  @MockK
  private lateinit var adjustmentService: AdjustmentService

  @MockK
  private lateinit var eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository

  @MockK
  private lateinit var projectTypeEntityRepository: ProjectTypeEntityRepository

  @InjectMockKs
  private lateinit var incentiveSchemeEventService: IncentiveSchemeEventService

  @Test
  fun `returns a sorted list of all appointments and adjustments for the CRN and event number in chronological order`() {
    val appointments = listOf(
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 4, 4),
        startTime = LocalTime.of(10, 0),
      ),
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 4, 4),
        startTime = LocalTime.of(8, 30),
      ),
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 3, 3),
        startTime = LocalTime.of(11, 30),
      ),
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 1, 1),
        startTime = LocalTime.of(9, 0),
      ),
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 2, 2),
        startTime = LocalTime.of(9, 0),
      ),
      AppointmentSummaryDto.valid().copy(
        date = LocalDate.of(2026, 1, 31),
        startTime = LocalTime.of(9, 0),
      ),
    )

    val adjustments = listOf(
      AdjustmentDto.valid().copy(
        date = LocalDate.of(2026, 3, 1),
      ),
      AdjustmentDto.valid().copy(
        date = LocalDate.of(2026, 2, 3),
      ),
    )

    every { appointmentService.getAppointments(crn = "X123456", toDate = LocalDate.now(), eventNumber = "1", pageable = Pageable.unpaged()) } returns PageImpl(appointments)
    every { adjustmentService.getAdjustments(crn = "X123456", eventNumber = 1) } returns adjustments
    every { eteCourseCompletionEventResolutionRepository.existsByDeliusAppointmentId(any()) } returns listOf(appointments[2].id)
    every { projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.ETE) } returns listOf(
      ProjectTypeEntity.valid().copy(projectTypeGroup = ProjectTypeGroup.ETE, code = appointments[3].projectTypeCode),
    )

    val result = incentiveSchemeEventService.getEvents("X123456", 1)

    assertThat(result).hasSize(8)
    assertThat(result[0]).isInstanceOf(IncentiveSchemeCourseCompletionAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Course completion appointment ${appointments[3].id}")
    assertThat(result[1]).isInstanceOf(IncentiveSchemeAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Appointment ${appointments[5].id}")
    assertThat(result[2]).isInstanceOf(IncentiveSchemeAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Appointment ${appointments[4].id}")
    assertThat(result[3]).isInstanceOf(IncentiveSchemeAdjustmentEvent::class.java).extracting { it.name }.isEqualTo("Adjustment ${adjustments[1].deliusId}")
    assertThat(result[4]).isInstanceOf(IncentiveSchemeAdjustmentEvent::class.java).extracting { it.name }.isEqualTo("Adjustment ${adjustments[0].deliusId}")
    assertThat(result[5]).isInstanceOf(IncentiveSchemeCourseCompletionAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Course completion appointment ${appointments[2].id}")
    assertThat(result[6]).isInstanceOf(IncentiveSchemeAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Appointment ${appointments[1].id}")
    assertThat(result[7]).isInstanceOf(IncentiveSchemeAppointmentEvent::class.java).extracting { it.name }.isEqualTo("Appointment ${appointments[0].id}")
  }
}

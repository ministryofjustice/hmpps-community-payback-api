package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toHttpParams
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AppointmentRetrievalServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var appointmentMappers: AppointmentMappers

  @RelaxedMockK
  private lateinit var contextService: ContextService

  @RelaxedMockK
  private lateinit var projectService: ProjectService

  @InjectMockKs
  private lateinit var service: AppointmentRetrievalService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val PROJECT_TYPE_CODE = "PROJTYPE123"
    const val USERNAME = "mr-user"
  }

  @BeforeEach
  fun setupUsernameContext() {
    every { contextService.getUserName() } returns USERNAME
  }

  @Nested
  inner class GetAppointment {

    @Test
    fun `if appointment not found, throw not found exception`() {
      every {
        communityPaybackAndDeliusClient.getAppointment(
          projectCode = PROJECT_CODE,
          appointmentId = 101L,
          username = USERNAME,
        )
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getAppointment(PROJECT_CODE, 101L)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Appointment not found for ID 'Project PROJ123, ID 101'")
    }

    @Test
    fun `appointment found`() {
      val appointment = NDAppointment.valid().copy(projectType = NDProjectType.valid().copy(code = PROJECT_TYPE_CODE))
      every { communityPaybackAndDeliusClient.getAppointment(PROJECT_CODE, 101L, USERNAME) } returns appointment

      val projectType = ProjectTypeEntity.valid()
      every { projectService.getProjectTypeForCode(PROJECT_TYPE_CODE) } returns projectType

      val appointmentDto = AppointmentDto.valid()
      every { appointmentMappers.toDto(appointment, projectType) } returns appointmentDto

      val result = service.getAppointment(PROJECT_CODE, 101L)

      assertThat(result).isSameAs(appointmentDto)
    }
  }

  @Nested
  inner class GetAppointments {

    @Test
    fun `should get appointments with crn`() {
      val crn = "CRN1"
      val pageable = PageRequest.of(0, 10)
      val ndAppointmentSummary = NDAppointmentSummary.valid()
      val pageResponse = PageResponse(
        content = listOf(ndAppointmentSummary),
        page = PageResponse.PageMeta(10, 0, 1, 1),
      )

      every {
        communityPaybackAndDeliusClient.getAppointments(
          crn = crn,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeCodes = null,
          params = pageable.toHttpParams(),
        )
      } returns pageResponse

      val appointmentSummaryDto = AppointmentSummaryDto.valid()
      every { appointmentMappers.toSummaryDto(ndAppointmentSummary) } returns appointmentSummaryDto

      val result = service.getAppointments(crn, null, null, null, null, null, pageable)

      assertThat(result.content).containsExactly(appointmentSummaryDto)
      assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `should get appointments without crn`() {
      val fromDate = LocalDate.now().minusDays(7)
      val toDate = LocalDate.now()
      val outcomeCodes = listOf("OUT1")
      val projectCodes = listOf("PROJ1")
      val projectTypeGroup = ProjectTypeGroupDto.GROUP
      val pageable = PageRequest.of(0, 10)

      val projectTypeDtos = listOf(ProjectTypeDto.valid().copy(code = "PT1"))
      every { projectService.projectTypesForGroup(projectTypeGroup) } returns projectTypeDtos

      val ndAppointmentSummary = NDAppointmentSummary.valid()
      val pageResponse = PageResponse(
        content = listOf(ndAppointmentSummary),
        page = PageResponse.PageMeta(10, 0, 1, 1),
      )

      every {
        communityPaybackAndDeliusClient.getAppointments(
          crn = null,
          fromDate = fromDate,
          toDate = toDate,
          outcomeCodes = outcomeCodes,
          projectCodes = projectCodes,
          projectTypeCodes = listOf("PT1"),
          params = pageable.toHttpParams(),
        )
      } returns pageResponse

      val appointmentSummaryDto = AppointmentSummaryDto.valid()
      every { appointmentMappers.toSummaryDto(ndAppointmentSummary) } returns appointmentSummaryDto

      val result = service.getAppointments(null, fromDate, toDate, outcomeCodes, projectCodes, projectTypeGroup, pageable)

      assertThat(result.content).containsExactly(appointmentSummaryDto)
    }
  }
}

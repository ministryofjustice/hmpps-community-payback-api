package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.DeliusAppointmentIdDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderNameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.AppointmentRetrievalService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.OffenderService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toMultiValueHttpParams
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.AppointmentMappers
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.ToAppointmentEntity.toAppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
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

  @RelaxedMockK
  private lateinit var offenderService: OffenderService

  @RelaxedMockK
  private lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @RelaxedMockK
  private lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

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
    fun `if appointment not found return null`() {
      every {
        communityPaybackAndDeliusClient.getAppointment(
          projectCode = PROJECT_CODE,
          appointmentId = 101L,
          username = USERNAME,
        )
      } throws WebClientResponseExceptionFactory.notFound()

      val result = service.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, 101L))

      assertThat(result).isNull()
    }

    @Test
    fun `appointment found`() {
      val deliusAppointment = NDAppointment.valid().copy(projectType = NDProjectType.valid().copy(code = PROJECT_TYPE_CODE))
      every { communityPaybackAndDeliusClient.getAppointment(PROJECT_CODE, 101L, USERNAME) } returns deliusAppointment

      val projectType = ProjectTypeEntity.valid()
      every { projectService.getProjectTypeForCode(PROJECT_TYPE_CODE) } returns projectType

      val appointmentEntity = AppointmentEntity.valid()
      every { appointmentEntityRepository.findByDeliusId(deliusAppointment.id) } returns appointmentEntity

      val appointmentDto = AppointmentDto.valid()
      every { appointmentMappers.toDto(deliusAppointment, appointmentEntity, projectType) } returns appointmentDto

      val result = service.getAppointment(DeliusAppointmentIdDto(PROJECT_CODE, 101L))

      assertThat(result).isSameAs(appointmentDto)
    }
  }

  @Nested
  inner class GetAppointments {

    @Test
    fun `should get appointments with crn and event number`() {
      val crn = "CRN1"
      val eventNumber = "2"
      val pageable = PageRequest.of(0, 10)
      val ndAppointmentSummary = NDAppointmentSummary.valid()
      val pageResponse = PageResponse(
        content = listOf(ndAppointmentSummary),
        page = PageResponse.PageMeta(10, 0, 1, 1),
      )

      every {
        communityPaybackAndDeliusClient.getAppointments(
          username = USERNAME,
          crn = crn,
          fromDate = null,
          toDate = null,
          outcomeCodes = null,
          projectCodes = null,
          projectTypeCodes = null,
          eventNumber = eventNumber,
          appointmentIds = null,
          params = pageable.toMultiValueHttpParams(),
        )
      } returns pageResponse

      val appointmentSummaryDto = AppointmentSummaryDto.valid()
      every { appointmentMappers.toSummaryDto(ndAppointmentSummary) } returns appointmentSummaryDto

      val result = service.getAppointments(
        crn = crn,
        fromDate = null,
        toDate = null,
        outcomeCodes = null,
        projectCodes = null,
        projectTypeGroup = null,
        eventNumber = eventNumber,
        pageable = pageable,
      )

      assertThat(result.content).containsExactly(appointmentSummaryDto)
      assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    fun `should get appointments without crn`() {
      val fromDate = LocalDate.now().minusDays(7)
      val toDate = LocalDate.now()
      val outcomeCodes = listOf("OUT1")
      val projectCodes = listOf("PROJ1")
      val projectTypeGroups = listOf(ProjectTypeGroupDto.GROUP, ProjectTypeGroupDto.INDIVIDUAL)
      val pageable = PageRequest.of(0, 10)

      val projectTypeDtos = listOf(ProjectTypeDto.valid().copy(code = "PT1"))
      every { projectService.projectTypesForGroup(ProjectTypeGroupDto.GROUP) } returns projectTypeDtos
      every { projectService.projectTypesForGroup(ProjectTypeGroupDto.INDIVIDUAL) } returns listOf(ProjectTypeDto.valid().copy(code = "PT2"))

      val ndAppointmentSummary = NDAppointmentSummary.valid()
      val pageResponse = PageResponse(
        content = listOf(ndAppointmentSummary),
        page = PageResponse.PageMeta(10, 0, 1, 1),
      )

      every {
        communityPaybackAndDeliusClient.getAppointments(
          username = USERNAME,
          crn = null,
          fromDate = fromDate,
          toDate = toDate,
          outcomeCodes = outcomeCodes,
          projectCodes = projectCodes,
          projectTypeCodes = listOf("PT1", "PT2"),
          eventNumber = null,
          appointmentIds = null,
          params = pageable.toMultiValueHttpParams(),
        )
      } returns pageResponse

      val appointmentSummaryDto = AppointmentSummaryDto.valid()
      every { appointmentMappers.toSummaryDto(ndAppointmentSummary) } returns appointmentSummaryDto

      val result = service.getAppointments(
        crn = null,
        fromDate = fromDate,
        toDate = toDate,
        outcomeCodes = outcomeCodes,
        projectCodes = projectCodes,
        projectTypeGroup = projectTypeGroups,
        pageable = pageable,
      )

      assertThat(result.content).containsExactly(appointmentSummaryDto)
    }

    @Test
    fun `should replace WITH_OUTCOME with all UPW outcome codes`() {
      val pageable = PageRequest.of(0, 10)
      val outcomes = listOf(
        ContactOutcomeEntity.valid().copy(code = "OUT1"),
        ContactOutcomeEntity.valid().copy(code = "OUT2"),
      )
      every { contactOutcomeEntityRepository.findAll() } returns outcomes
      every {
        communityPaybackAndDeliusClient.getAppointments(
          username = USERNAME,
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = listOf("OUT1", "OUT2", "NO_OUTCOME"),
          projectCodes = null,
          projectTypeCodes = null,
          eventNumber = null,
          appointmentIds = null,
          params = pageable.toMultiValueHttpParams(),
        )
      } returns PageResponse(emptyList(), PageResponse.PageMeta(10, 0, 0, 0))

      service.getAppointments(
        outcomeCodes = listOf("WITH_OUTCOME", "NO_OUTCOME"),
        pageable = pageable,
      )

      verify(exactly = 1) {
        communityPaybackAndDeliusClient.getAppointments(
          username = USERNAME,
          crn = null,
          fromDate = null,
          toDate = null,
          outcomeCodes = listOf("OUT1", "OUT2", "NO_OUTCOME"),
          projectCodes = null,
          projectTypeCodes = null,
          eventNumber = null,
          appointmentIds = null,
          params = pageable.toMultiValueHttpParams(),
        )
      }
    }
  }

  @Nested
  inner class GetOrCreateAppointmentEntity {

    @Test
    fun `doesnt exist, save new entity`() {
      val existingAppointment = AppointmentDto.valid()
      val projectType = ProjectTypeEntity.valid()
      val project = ProjectDto.valid().copy(projectCode = existingAppointment.projectCode, projectType = projectType.toDto())

      every { appointmentEntityRepository.findByDeliusId(existingAppointment.id) } returns null
      every { appointmentEntityRepository.save(existingAppointment.toAppointmentEntity(null, null, null)) } returnsArgument 0
      every { projectService.getProject(existingAppointment.projectCode) } returns project
      every { projectService.getProjectTypeForCode(projectType.code) } returns projectType

      val name = OffenderNameDto.valid()

      every { offenderService.getNameIgnoringLimitedStatus(existingAppointment.offender.crn) } returns name

      val result = service.getOrCreateAppointmentEntity(existingAppointment)

      assertThat(result).isEqualTo(existingAppointment.toAppointmentEntity(null, null, null))
      assertThat(result.projectType).isEqualTo(projectType)
      assertThat(result.firstName).isEqualTo(name.forename)
      assertThat(result.lastName).isEqualTo(name.surname)
    }

    @Test
    fun `does exist, update date and return updated version`() {
      val existingAppointment = AppointmentDto.valid().copy(date = LocalDate.of(2022, 2, 2))
      val existingEntity = AppointmentEntity.valid().copy(date = LocalDate.of(2021, 1, 1))

      every { appointmentEntityRepository.findByDeliusId(existingAppointment.id) } returns existingEntity
      every { appointmentEntityRepository.save(existingEntity) } returnsArgument 0

      val result = service.getOrCreateAppointmentEntity(existingAppointment)

      assertThat(result.date).isEqualTo(LocalDate.of(2022, 2, 2))
    }

    @Test
    fun `does exist, update project type and return updated version`() {
      val existingAppointment = AppointmentDto.valid().copy(date = LocalDate.of(2022, 2, 2))
      val existingEntity = AppointmentEntity.valid().copy(date = LocalDate.of(2021, 1, 1))

      every { appointmentEntityRepository.findByDeliusId(existingAppointment.id) } returns existingEntity
      every { appointmentEntityRepository.save(existingEntity) } returnsArgument 0

      val projectType = ProjectTypeEntity.valid()
      val project = ProjectDto.valid().copy(projectCode = existingAppointment.projectCode, projectType = projectType.toDto())

      every { projectService.getProject(existingAppointment.projectCode) } returns project
      every { projectService.getProjectTypeForCode(projectType.code) } returns projectType

      val result = service.getOrCreateAppointmentEntity(existingAppointment)

      assertThat(result.projectType).isEqualTo(projectType)
    }

    @Test
    fun `does exist, update first and last name and return updated version`() {
      val existingAppointment = AppointmentDto.valid().copy(date = LocalDate.of(2022, 2, 2))
      val existingEntity = AppointmentEntity.valid().copy(date = LocalDate.of(2021, 1, 1))

      every { appointmentEntityRepository.findByDeliusId(existingAppointment.id) } returns existingEntity
      every { appointmentEntityRepository.save(existingEntity) } returnsArgument 0

      val name = OffenderNameDto.valid()

      every { offenderService.getNameIgnoringLimitedStatus(existingAppointment.offender.crn) } returns name

      val result = service.getOrCreateAppointmentEntity(existingAppointment)

      assertThat(result.firstName).isEqualTo(name.forename)
      assertThat(result.lastName).isEqualTo(name.surname)
    }
  }
}

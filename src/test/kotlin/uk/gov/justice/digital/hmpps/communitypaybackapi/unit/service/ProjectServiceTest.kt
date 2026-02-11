package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectOutcomeSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto.INDIVIDUAL
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ProjectService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.unit.util.WebClientResponseExceptionFactory

@ExtendWith(MockKExtension::class)
class ProjectServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var projectTypeEntityRepository: ProjectTypeEntityRepository

  @InjectMockKs
  private lateinit var service: ProjectService

  private companion object {
    const val PROJECT_CODE = "PROJ123"
    const val PROJECT_TYPE_CODE = "PROJTYPE"
  }

  @Nested
  inner class GetProjects {

    @Test
    fun `should get project codes with paging and sorting`() {
      val providerCode = "N01"
      val teamCode = "T01"
      val pageNumber = 0
      val pageSize = 20

      val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "projectName"))
      val ndProjectOutcomeSummary = NDProjectOutcomeSummary.valid()

      every {
        communityPaybackAndDeliusClient.getProjects(
          providerCode = providerCode,
          teamCode = teamCode,
          projectTypeCodes = listOf("PJ01"),
          params = mapOf("page" to pageNumber.toString(), "size" to pageSize.toString(), "sort" to "projectName,desc"),
        )
      } returns PageResponse(
        content = listOf(ndProjectOutcomeSummary),
        page = PageResponse.PageMeta(totalElements = 1, totalPages = 1, size = 20, number = 0),
      )

      every {
        projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.INDIVIDUAL)
      } returns listOf(ProjectTypeEntity.valid().copy(code = "PJ01"))

      val projectsPageResponse: Page<ProjectOutcomeSummaryDto> = service.getProjects(providerCode, teamCode, INDIVIDUAL, pageable)
      assertThat(projectsPageResponse.content).hasSize(1)
      assertThat(projectsPageResponse.totalElements).isEqualTo(1)
      assertThat(projectsPageResponse.totalPages).isEqualTo(1)
      assertThat(projectsPageResponse.number).isEqualTo(pageNumber)
      assertThat(projectsPageResponse.size).isEqualTo(pageSize)
      assertThat(projectsPageResponse.content[0].projectName).isEqualTo(ndProjectOutcomeSummary.name)
      assertThat(projectsPageResponse.content[0].projectCode).isEqualTo(ndProjectOutcomeSummary.code)
    }
  }

  @Nested
  inner class GetProject {

    @Test
    fun `if project not found, throw not found exception`() {
      every {
        communityPaybackAndDeliusClient.getProject(
          projectCode = PROJECT_CODE,
        )
      } throws WebClientResponseExceptionFactory.notFound()

      assertThatThrownBy {
        service.getProject(PROJECT_CODE)
      }.isInstanceOf(NotFoundException::class.java).hasMessage("Project not found for ID 'PROJ123'")
    }

    @Test
    fun `throw exception if project type can't be resolved`() {
      val project = NDProject.valid().copy(type = NDProjectType.valid().copy(code = PROJECT_TYPE_CODE))

      every { communityPaybackAndDeliusClient.getProject(PROJECT_CODE) } returns project
      every { projectTypeEntityRepository.getByCode(PROJECT_TYPE_CODE) } returns null

      assertThatThrownBy {
        service.getProject(PROJECT_CODE)
      }.isInstanceOf(RuntimeException::class.java).hasMessage("could not find project type for code 'PROJTYPE'")
    }

    @Test
    fun `project found`() {
      val project = NDProject.valid().copy(type = NDProjectType.valid().copy(code = PROJECT_TYPE_CODE))
      val projectType = ProjectTypeEntity.valid()

      every { communityPaybackAndDeliusClient.getProject(PROJECT_CODE) } returns project
      every { projectTypeEntityRepository.getByCode(PROJECT_TYPE_CODE) } returns projectType

      val result = service.getProject(PROJECT_CODE)

      assertThat(result).isEqualTo(project.toDto(projectType))
    }
  }
}

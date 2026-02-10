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
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
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
      val project = NDProject.valid().copy(projectTypeCode = PROJECT_TYPE_CODE)

      every { communityPaybackAndDeliusClient.getProject(PROJECT_CODE) } returns project
      every { projectTypeEntityRepository.getByCode(PROJECT_TYPE_CODE) } returns null

      assertThatThrownBy {
        service.getProject(PROJECT_CODE)
      }.isInstanceOf(RuntimeException::class.java).hasMessage("could not find project type for code 'PROJTYPE'")
    }

    @Test
    fun `project found`() {
      val project = NDProject.valid().copy(projectTypeCode = PROJECT_TYPE_CODE)
      val projectType = ProjectTypeEntity.valid()

      every { communityPaybackAndDeliusClient.getProject(PROJECT_CODE) } returns project
      every { projectTypeEntityRepository.getByCode(PROJECT_TYPE_CODE) } returns projectType

      val result = service.getProject(PROJECT_CODE)

      assertThat(result).isEqualTo(project.toDto(projectType))
    }
  }
}

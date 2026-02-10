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
    fun `project found`() {
      val project = NDProject.valid()
      every { communityPaybackAndDeliusClient.getProject(PROJECT_CODE) } returns project

      val result = service.getProject(PROJECT_CODE)

      assertThat(result).isEqualTo(project.toDto())
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto

@Service
class ProjectService(
  private val projectTypeEntityRepository: ProjectTypeEntityRepository,
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
) {
  fun getProjectTypeForCode(code: String) = projectTypeEntityRepository.getByCode(code)

  fun projectTypesForGroup(projectTypeGroup: ProjectTypeGroupDto) = projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.fromDto(projectTypeGroup)).map { it.toDto() }

  fun getProject(projectCode: String): ProjectDto {
    val project = try {
      communityPaybackAndDeliusClient.getProject(
        projectCode = projectCode,
      )
    } catch (_: WebClientResponseException.NotFound) {
      throw NotFoundException("Project", projectCode)
    }

    val projectTypeCode = project.projectTypeCode
    val projectType = projectTypeEntityRepository.getByCode(projectTypeCode)
      ?: error("could not find project type for code '$projectTypeCode'")

    return project.toDto(projectType)
  }
}

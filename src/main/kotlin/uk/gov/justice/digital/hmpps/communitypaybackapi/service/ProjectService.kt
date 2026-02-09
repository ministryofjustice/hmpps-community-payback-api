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

  fun projectTypesForGroup(projectTypeGroup: ProjectTypeGroupDto) = projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.fromDto(projectTypeGroup)).map { it.toDto() }

  fun getProject(projectCode: String): ProjectDto = try {
    communityPaybackAndDeliusClient.getProject(
      projectCode = projectCode,
    ).toDto()
  } catch (_: WebClientResponseException.NotFound) {
    throw NotFoundException("Project", projectCode)
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.toQueryTriplet
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.mappers.toNDProjectTypeCodes

@Service
class ProjectService(
  val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val projectTypeEntityRepository: ProjectTypeEntityRepository,
) {
  private companion object
  fun getProjectTypeForCode(code: String) = projectTypeEntityRepository.getByCode(code)

  fun getProjects(
    providerCode: String,
    teamCode: String,
    projectTypeGroup: ProjectTypeGroupDto?,
    pageable: Pageable,
  ): Page<ProjectSummaryDto> {
    val (page, size, sortValues) = pageable.toQueryTriplet()
    val pageResponse = communityPaybackAndDeliusClient.getProjects(
      providerCode = providerCode,
      teamCode = teamCode,
      projectTypeCodes = projectTypeGroup?.toNDProjectTypeCodes(),
      page = page,
      size = size,
      sort = sortValues,
    )
    return PageImpl(pageResponse.content.map { it.toDto() }, pageable, pageResponse.page.totalElements)
  }

  fun projectTypesForGroup(projectTypeGroup: ProjectTypeGroupDto) =
    projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.fromDto(projectTypeGroup))
      .map { it.toDto() }

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

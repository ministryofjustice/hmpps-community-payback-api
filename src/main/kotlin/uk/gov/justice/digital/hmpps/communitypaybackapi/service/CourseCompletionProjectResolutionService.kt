package uk.gov.justice.digital.hmpps.communitypaybackapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.ProjectTypeGroupDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup

@Service
class CourseCompletionProjectResolutionService(
  private val communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient,
  private val projectTypeEntityRepository: ProjectTypeEntityRepository,
) {
  fun resolveProjectCode(event: EteCourseCompletionEventEntity, teamCode: String): String? {
    val projects = getEteProjects(event.pdu.providerCode, teamCode)
    if (projects.isEmpty()) return null

    return projects.resolveRegionalProject(event, teamCode)?.code
  }

  private fun List<NDProject>.resolveRegionalProject(event: EteCourseCompletionEventEntity, teamCode: String) = when (event.region) {
    "East Midlands" -> resolveEastMidlandsProject(event)
    "East of England" -> resolveEastOfEnglandProject(event)
    "Greater Manchester" -> resolveGreaterManchesterProject(event)
    "Kent, Surrey and Sussex" -> matchProjectName(event.courseName)
    "London" -> matchProjectName("20% ETE Standalone HMPPS Portal")
    "North East" -> resolveNorthEastProject(event)
    "North West" -> resolveNorthWestProject(teamCode, event.office)
    "South Central" -> resolveSouthCentralProject(event)
    "South West" -> resolveSouthWestProject(event)
    "Wales" -> resolveWalesProject(event)
    "West Midlands" -> matchProjectName("ET5 ETE HMPPS Portal")
    "Yorks & Humber" -> resolveYorksAndHumberProject(event)
    else -> null
  }

  private fun List<NDProject>.resolveEastMidlandsProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: when {
    event.isMoodle() -> matchProjectName("ET5 ETE HMPPS portal")
    event.isAlison() -> matchProjectName("Alison.com")
    else -> null
  }

  private fun List<NDProject>.resolveEastOfEnglandProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: when {
    event.isAlison() -> matchProjectName("ETE Alison.com")
    else -> null
  }

  private fun List<NDProject>.resolveGreaterManchesterProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: when {
    event.isAlison() -> matchProjectName("Alison Community Campus")
    else -> null
  }

  private fun List<NDProject>.resolveNorthEastProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: when {
    event.isAlison() -> matchProjectName("ETE HMPPS Portal Alison.com")
    else -> null
  }

  private fun List<NDProject>.resolveSouthCentralProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: matchProjectName("ET5 ETE HMPPS Portal")

  private fun List<NDProject>.resolveSouthWestProject(event: EteCourseCompletionEventEntity) = matchProjectName(event.courseName) ?: when {
    event.isAlison() -> matchProjectName("Alison")
    else -> null
  }

  private fun List<NDProject>.resolveWalesProject(event: EteCourseCompletionEventEntity) = when {
    event.isMandatory() -> matchProjectName("Mandatory ETE")
    else -> matchProjectName("ETE E-Learning")
  }

  private fun List<NDProject>.resolveYorksAndHumberProject(event: EteCourseCompletionEventEntity) = when {
    event.isYorksAndHumberMandatory() -> matchProjectName(event.courseName) ?: matchProjectName("mandatory")
    event.isAlison() -> matchProjectName("ETE Alison.com Course")
    else -> matchProjectName("ET5 ETE HMPPS Portal Other")
  }

  private fun getEteProjects(providerCode: String, teamCode: String): List<NDProject> {
    val eteProjectTypeCodes =
      projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.fromDto(ProjectTypeGroupDto.ETE))
        .map { it.code }

    return communityPaybackAndDeliusClient.getProjects(
      providerCode = providerCode,
      teamCode = teamCode,
      typeCode = eteProjectTypeCodes,
      params = mapOf("page" to "0", "size" to "500", "sort" to "name,asc"),
    ).content.map { it.project }
  }

  private fun List<NDProject>.matchProjectName(name: String): NDProject? {
    val target = name.normalizedWords()
    return closestMatch(filter { it.name.normalizedWords().contains(target) })
      ?: closestMatch(filter { it.name.significantWords().containsAll(target.significantWords()) })
  }

  private fun closestMatch(projects: List<NDProject>) = projects.minWithOrNull(compareBy<NDProject> { it.name.normalizedWords().length }.thenBy { it.name })

  private fun List<NDProject>.resolveNorthWestProject(teamCode: String, office: String): NDProject? {
    val projectName = when (teamCode) {
      "N51CWP" -> if (office.lowercase() == "winsford office") {
        "ETE Community Portal Winsford"
      } else {
        "ETE Community Portal Ellesmere Port/Chester"
      }
      "N51CEP" -> if (office.lowercase() == "crewe office") {
        "ETE Community Portal Crewe"
      } else {
        "ETE Community Portal Macclesfield"
      }
      else -> "ETE"
    }
    return matchProjectName(projectName)
  }

  private fun String.normalizedWords() = lowercase()
    .replace("&", " and ")
    .replace(Regex("[^a-z0-9]+"), " ")
    .replace(Regex("\\b(introduction|intro)\\b"), "")
    .replace(Regex("\\bcustomer services\\b"), "customer service")
    .replace(Regex("\\bindustrial and commercial cleaning\\b"), "industrial cleaning")
    .replace(Regex("\\bgardening\\b"), "garden")
    .replace(Regex("\\bit\\b"), "")
    .trim()
    .replace(Regex("\\s+"), " ")

  private fun String.significantWords() = normalizedWords()
    .split(" ")
    .filter { it.isNotBlank() && it !in setOf("a", "and", "in", "of", "the", "to") }
    .toSet()

  private fun EteCourseCompletionEventEntity.isMoodle() = this.provider.lowercase() == "moodle"

  private fun EteCourseCompletionEventEntity.isAlison() = this.provider.lowercase() == "alison"

  private fun EteCourseCompletionEventEntity.isMandatory() = this.courseType.lowercase() == "mandatory" ||
    this.courseName.normalizedWords() in mandatoryCourseNames

  private fun EteCourseCompletionEventEntity.isYorksAndHumberMandatory() = this.courseType.lowercase() == "mandatory" ||
    this.courseName.normalizedWords() in mandatoryCourseNames ||
    (this.courseName.normalizedWords() == "employability" && this.courseType.normalizedWords().contains("unemployed"))

  companion object {
    private val mandatoryCourseNames = setOf(
      "health and safety",
      "manual handling",
      "first aid",
    )
  }
}

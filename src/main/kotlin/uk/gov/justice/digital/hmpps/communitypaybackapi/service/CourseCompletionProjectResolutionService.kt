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

    return when (event.region) {
      "East Midlands" -> projects.matchCourse(event) ?: when {
        event.isMoodle() -> projects.matchProjectName("ET5 ETE HMPPS portal")
        event.isAlison() -> projects.matchProjectName("Alison.com")
        else -> null
      }

      "East of England" -> projects.matchCourse(event) ?: when {
        event.isAlison() -> projects.matchProjectName("ETE Alison.com")
        else -> null
      }

      "Greater Manchester" -> projects.matchCourse(event) ?: when {
        event.isAlison() -> projects.matchProjectName("Alison Community Campus")
        else -> null
      }

      "Kent, Surrey and Sussex" -> projects.matchCourse(event)
      "London" -> projects.matchProjectName("20% ETE Standalone HMPPS Portal")
      "North East" -> projects.matchCourse(event) ?: when {
        event.isAlison() -> projects.matchProjectName("ETE HMPPS Portal Alison.com")
        else -> null
      }

      "North West" -> projects.resolveNorthWestProject(teamCode, event.office)
      "South Central" -> projects.matchCourse(event)
        ?: projects.matchProjectName("ET5 ETE HMPSS Portal")
        ?: projects.matchProjectName("ET5 ETE HMPPS Portal")

      "South West" -> projects.matchCourse(event) ?: when {
        event.isAlison() -> projects.matchProjectName("Alison")
        else -> null
      }

      "Wales" -> when {
        event.isMandatory() -> projects.matchProjectName("Mandatory ETE")
        else -> projects.matchProjectName("ETE E-Learning")
      }

      "West Midlands" -> projects.matchProjectName("ET5 ETE HMPPS Portal")
      "Yorks & Humber" -> when {
        event.isYorksAndHumberMandatory() -> projects.matchCourse(event)
          ?: projects.matchProjectName("mandatory")

        event.isAlison() -> projects.matchProjectName("ETE Alison.com Course")
        else -> projects.matchProjectName("ET5 ETE HMPPS Portal Other")
      }

      else -> null
    }?.code
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

  private fun List<NDProject>.matchCourse(event: EteCourseCompletionEventEntity): NDProject? =
    matchProjectName(event.courseName)

  private fun List<NDProject>.matchProjectName(name: String): NDProject? {
    val target = name.normalizedWords()
    return closestMatch(filter { it.name.normalizedWords().contains(target) })
      ?: closestMatch(filter { it.name.normalizedWordSet().containsAll(target.significantWords()) })
  }

  private fun closestMatch(projects: List<NDProject>) =
    projects.minWithOrNull(compareBy<NDProject> { it.name.normalizedWords().length }.thenBy { it.name })

  private fun List<NDProject>.resolveNorthWestProject(teamCode: String, office: String): NDProject? {
    val projectName = when (teamCode) {
      "N51CBH" -> "Preston ETE"
      "N51CAH" -> "Barrow ETE"
      "N51CAJ" -> "Blackburn ETE"
      "N51CAO" -> "Burnley ETE"
      "N51CAP" -> "Carlisle ETE"
      "N51LNP" -> "ETE Community Portal"
      "N51CWP" -> if (office.equals(
          "Winsford Office",
          ignoreCase = true,
        )
      ) "ETE Community Portal Winsford" else "ETE Community Portal Ellesmere Port/Chester"

      "N51CAV" -> "Chorley ETE"
      "N51CAK" -> "Blackpool ETE"
      "N51CEP" -> if (office.equals(
          "Crewe Office",
          ignoreCase = true,
        )
      ) "ETE Community Portal Crewe" else "ETE Community Portal Macclesfield"

      "N51CAB" -> "Accrington ETE"
      "N51CBB" -> "Kendal ETE"
      "N51CBD" -> "Lancaster ETE"
      "N51KCP" -> "ETE Community Portal"
      "N51HWP" -> "ETE Community Portal"
      "N51CBM" -> "Skelmersdale ETE"
      "N51SCP" -> "ETE Community Portal"
      "N51WCP" -> "ETE Community Portal"
      "N51CBP" -> "Workington ETE"
      else -> null
    }

    return projectName?.let { matchProjectName(it) }
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

  private fun String.normalizedWordSet() = significantWords()

  private fun EteCourseCompletionEventEntity.isMoodle() = this.provider.lowercase() == "moodle"

  private fun EteCourseCompletionEventEntity.isAlison() = this.provider.lowercase() == "alison"

  private fun EteCourseCompletionEventEntity.isMandatory() =
    this.courseType.lowercase() == "mandatory" || this.courseName.normalizedWords() in mandatoryCourseNames

  private fun EteCourseCompletionEventEntity.isYorksAndHumberMandatory() =
    this.courseType.lowercase() == "mandatory" ||
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

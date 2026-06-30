package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CommunityPaybackAndDeliusClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectOutcomeStats
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeGroup
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.CourseCompletionProjectResolutionService
import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class CourseCompletionProjectResolutionServiceTest {

  @RelaxedMockK
  lateinit var communityPaybackAndDeliusClient: CommunityPaybackAndDeliusClient

  @RelaxedMockK
  lateinit var projectTypeEntityRepository: ProjectTypeEntityRepository

  private lateinit var service: CourseCompletionProjectResolutionService

  @BeforeEach
  fun setUp() {
    service = CourseCompletionProjectResolutionService(
      communityPaybackAndDeliusClient,
      projectTypeEntityRepository,
    )

    every { projectTypeEntityRepository.findByProjectTypeGroupOrderByCodeAsc(ProjectTypeGroup.ETE) } returns listOf(
      ProjectTypeEntity.valid().copy(code = "ET5"),
      ProjectTypeEntity.valid().copy(code = "UP06"),
    )
  }

  @Nested
  inner class ResolveProjectCode {

    @Test
    fun `gets ETE projects for the resolved UPW team`() {
      val event = event(region = "East Midlands", courseName = "Painting and Decorating")
      stubProjects(project("ETE - Painting & Decorating", "PROJ1"))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo("PROJ1")
      verify {
        communityPaybackAndDeliusClient.getProjects(
          providerCode = event.pdu.providerCode,
          teamCode = "TEAM1",
          typeCode = listOf("ET5", "UP06"),
          params = mapOf("page" to "0", "size" to "500", "sort" to "name,asc"),
        )
      }
    }

    @Test
    fun `matches project name to course name flexibly`() {
      val event = event(region = "East of England", courseName = "Health and Safety in Construction")
      stubProjects(project("ETE Portal - Health & Safety in Construction", "MATCH"))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo("MATCH")
    }

    @ParameterizedTest(name = "East Midlands course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#eastMidlandsCourseMappings")
    fun `resolves East Midlands course projects and Moodle fallback`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "East Midlands", provider = "Moodle", courseName = courseName)
      stubProjects(*eastMidlandsProjects())

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses East Midlands Alison fallback when course project is not found`() {
      val event = event(region = "East Midlands", provider = "Alison", courseName = "Introduction to Supervision")
      stubProjects(*eastMidlandsProjects())

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("Alison.com"))
    }

    @ParameterizedTest(name = "East of England course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#eastOfEnglandCourseMappings")
    fun `resolves East of England course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "East of England", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(eastOfEngland))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses East of England Alison fallback when course project is not found`() {
      val event = event(region = "East of England", provider = "Alison", courseName = "Introduction to Supervision")
      stubProjects(*projectsFor(eastOfEngland))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("ETE - Alison.com"))
    }

    @ParameterizedTest(name = "Greater Manchester course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#greaterManchesterCourseMappings")
    fun `resolves Greater Manchester course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "Greater Manchester", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(greaterManchester))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses Greater Manchester Alison fallback when course project is not found`() {
      val event = event(region = "Greater Manchester", provider = "Alison", courseName = "Introduction to Supervision")
      stubProjects(*projectsFor(greaterManchester))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("Alison - Community Campus"))
    }

    @ParameterizedTest(name = "Kent, Surrey and Sussex course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#kentSurreyAndSussexCourseMappings")
    fun `resolves Kent Surrey and Sussex course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "Kent, Surrey and Sussex", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(kentSurreyAndSussex))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @ParameterizedTest(name = "North East course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#northEastCourseMappings")
    fun `resolves North East course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "North East", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(northEast))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses North East Alison fallback when course project is not found`() {
      val event = event(region = "North East", provider = "Alison", courseName = "Introduction to Supervision")
      stubProjects(*projectsFor(northEast))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("ETE - HMPPS Portal - Alison.com"))
    }

    @ParameterizedTest(name = "South Central course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#southCentralCourseMappings")
    fun `resolves South Central course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "South Central", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(southCentral))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses South Central portal fallback when course project is not found`() {
      val event = event(region = "South Central", provider = "Moodle", courseName = "Introduction to Supervision")
      stubProjects(*projectsFor(southCentral))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("ET5 ETE – HMPPS portal"))
    }

    @ParameterizedTest(name = "South West course {0} resolves to {1}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#southWestCourseMappings")
    fun `resolves South West course projects`(
      courseName: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "South West", provider = "Moodle", courseName = courseName)
      stubProjects(*projectsFor(southWest))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `uses South West Alison fallback when course project is not found`() {
      val event = event(region = "South West", provider = "Alison", courseName = "Introduction to Supervision")
      stubProjects(*projectsFor(southWest))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor("ETE CC Alison Bath"))
    }

    @Test
    fun `always uses London standalone portal project`() {
      val event = event(region = "London", courseName = "Food Hygiene")
      stubProjects(
        project("Food Hygiene", "COURSE"),
        project("20% ETE Standalone HMPPS Portal", "LONDON"),
      )

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo("LONDON")
    }

    @ParameterizedTest(name = "North West team {0} and office {1} resolves to {2}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#northWestMappings")
    fun `resolves North West team and office projects`(
      teamCode: String,
      office: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "North West", office = office)
      stubProjects(*northWestProjects())

      val result = service.resolveProjectCode(event, teamCode)

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @ParameterizedTest(name = "Wales course {0} with type {1} resolves to {2}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#walesMappings")
    fun `resolves Wales projects`(
      courseName: String,
      courseType: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "Wales", courseName = courseName, courseType = courseType)
      stubProjects(
        project("Mandatory ETE", projectCodeFor("Mandatory ETE")),
        project("ETE - E-Learning", projectCodeFor("ETE - E-Learning")),
      )

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @ParameterizedTest(name = "Yorks & Humber course {0}, type {1}, provider {2} resolves to {3}")
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.CourseCompletionProjectResolutionServiceTest#yorksAndHumberMappings")
    fun `resolves Yorks and Humber projects`(
      courseName: String,
      courseType: String,
      provider: String,
      expectedProjectName: String,
    ) {
      val event = event(region = "Yorks & Humber", provider = provider, courseName = courseName, courseType = courseType)
      stubProjects(*projectsFor(yorksAndHumber))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isEqualTo(projectCodeFor(expectedProjectName))
    }

    @Test
    fun `returns null when no region rule resolves a project`() {
      val event = event(region = "Kent, Surrey and Sussex", courseName = "Food Hygiene")
      stubProjects(project("CC Introduction to Retail", "RETAIL"))

      val result = service.resolveProjectCode(event, "TEAM1")

      assertThat(result).isNull()
    }
  }

  private fun event(
    region: String,
    provider: String = "Moodle",
    courseName: String = "Food Hygiene",
    courseType: String = "Certified",
    office: String = "Office 1",
  ) = EteCourseCompletionEventEntity.valid().copy(
    region = region,
    provider = provider,
    courseName = courseName,
    courseType = courseType,
    office = office,
  )

  private fun project(name: String, code: String) = NDProject.valid().copy(name = name, code = code)

  private fun eastMidlandsProjects() = projectsFor(eastMidlands)

  private fun northWestProjects() = northWest
    .map { it.projectName }
    .distinct()
    .map { project(it, projectCodeFor(it)) }
    .toTypedArray()

  private fun projectsFor(courseToProjects: List<CourseToProject>) = courseToProjects
    .map { it.projectName }
    .distinct()
    .map { project(it, projectCodeFor(it)) }
    .toTypedArray()

  private fun projectCodeFor(projectName: String) = projectName
    .uppercase()
    .replace(Regex("[^A-Z0-9]+"), "_")
    .trim('_')

  private fun stubProjects(vararg projects: NDProject) {
    every {
      communityPaybackAndDeliusClient.getProjects(any(), any(), any(), any())
    } returns PageResponse(
      content = projects.map { NDProjectOutcomeStats.valid().copy(project = it) },
      page = PageResponse.PageMeta(size = projects.size, number = 0, totalElements = projects.size.toLong(), totalPages = 1),
    )
  }

  companion object {
    @JvmStatic
    fun eastMidlandsCourseMappings(): Stream<Arguments> = courseMappingsFor(eastMidlands)

    @JvmStatic
    fun eastOfEnglandCourseMappings(): Stream<Arguments> = courseMappingsFor(eastOfEngland)

    @JvmStatic
    fun greaterManchesterCourseMappings(): Stream<Arguments> = courseMappingsFor(greaterManchester)

    @JvmStatic
    fun kentSurreyAndSussexCourseMappings(): Stream<Arguments> = courseMappingsFor(kentSurreyAndSussex)

    @JvmStatic
    fun northEastCourseMappings(): Stream<Arguments> = courseMappingsFor(northEast)

    @JvmStatic
    fun northWestMappings(): Stream<Arguments> = northWest.map {
      Arguments.of(it.teamCode, it.office, it.projectName)
    }.stream()

    @JvmStatic
    fun southCentralCourseMappings(): Stream<Arguments> = courseMappingsFor(southCentral)

    @JvmStatic
    fun southWestCourseMappings(): Stream<Arguments> = courseMappingsFor(southWest)

    @JvmStatic
    fun walesMappings(): Stream<Arguments> = Stream.of(
      Arguments.of("Health & Safety", "Certified", "Mandatory ETE"),
      Arguments.of("Manual Handling", "Certified", "Mandatory ETE"),
      Arguments.of("First Aid", "Certified", "Mandatory ETE"),
      Arguments.of("Introduction to Digital", "Mandatory", "Mandatory ETE"),
      Arguments.of("Introduction to Digital", "Certified", "ETE - E-Learning"),
    )

    @JvmStatic
    fun yorksAndHumberMappings(): Stream<Arguments> = Stream.of(
      Arguments.of("Employability", "Unemployed", "Moodle", "ETE HMPPS Portal - Employability - mandatory if unemployed"),
      Arguments.of("Employability", "Certified", "Moodle", "ET5 ETE – HMPPS Portal - Other"),
      Arguments.of("First Aid", "Certified", "Moodle", "ETE HMPPS Portal - First Aid - mandatory"),
      Arguments.of("Health and Safety", "Certified", "Moodle", "ETE HMPPS Portal - Health and Safety - mandatory"),
      Arguments.of("Manual Handling", "Certified", "Moodle", "ETE HMPPS Portal - Manual Handling - mandatory"),
      Arguments.of("Introduction to Digital", "Certified", "Moodle", "ET5 ETE – HMPPS Portal - Other"),
      Arguments.of("Introduction to Supervision", "Certified", "Alison", "ETE Alison.com Course"),
    )

    private fun courseMappingsFor(courseToProjects: List<CourseToProject>) = courseToProjects
      .filter { it.courseName != null }
      .map { Arguments.of(it.courseName, it.projectName) }
      .stream()

    private data class CourseToProject(
      val courseName: String?,
      val projectName: String,
    )

    private data class NorthWestCase(
      val teamCode: String,
      val office: String = "Office 1",
      val projectName: String,
    )

    private val eastMidlands = listOf(
      CourseToProject(null, "Alison.com"),
      CourseToProject("Introduction to Digital", "ETE - Introduction to Digital"),
      CourseToProject("Introduction to Retail", "ETE - Introduction to Retail"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "ETE – Introduction to Industrial and Commercial Cleaning"),
      CourseToProject("Health and Safety in Construction", "ETE - Health & Safety in Construction"),
      CourseToProject("Introduction to Self-Employment", "ETE – Introduction to self-employment"),
      CourseToProject("Introduction to Plumbing", "ETE – Introduction to Plumbing"),
      CourseToProject("Effective Communication", "ETE - Effective Communication"),
      CourseToProject("Introduction to Electrical", "ETE – Introduction to Electrical"),
      CourseToProject("Building your path: A Career in Construction", "ETE - Building your Path: A Career in Construction"),
      CourseToProject("Employability", "ETE - Employability"),
      CourseToProject("Food Hygiene", "ETE - Food Hygiene"),
      CourseToProject("Money Management", "ETE - Money Management"),
      CourseToProject("Adult Social Care", "ETE - Adult Social Care"),
      CourseToProject("First Aid", "ETE - First Aid"),
      CourseToProject("Manual Handling", "ETE - Manual Handling"),
      CourseToProject("Health and Safety", "ETE - Health & Safety"),
      CourseToProject("Customer Service", "ETE - Introduction to Customer Service"),
      CourseToProject("Gardening and Horticulture", "ETE - Introduction to Gardening & Horticulture"),
      CourseToProject("Painting and Decorating", "ETE - Painting & Decorating"),
      CourseToProject("Mental Health Awareness", "ETE - Mental Health Awareness"),
      CourseToProject("Introduction to Supervision", "ET5 ETE – HMPPS portal"),
      CourseToProject("Diploma in Basic English Grammar", "ET5 ETE – HMPPS portal"),
      CourseToProject("Comprehensive Car Mechanic", "ET5 ETE – HMPPS portal"),
    )

    private val eastOfEngland = listOf(
      CourseToProject(null, "ETE - Alison.com"),
      CourseToProject("Adult Social Care", "ETE Portal - Adult Social Care"),
      CourseToProject("Building your path: A Career in Construction", "ETE Portal - Building Your Path: A Career in Construction"),
      CourseToProject("Effective Communication", "ETE Portal - Effective Communication"),
      CourseToProject("Employability", "ETE Portal - Employability"),
      CourseToProject("Food Hygiene", "ETE Portal - Food Hygiene"),
      CourseToProject("Health and Safety in Construction", "ETE Portal - Health & Safety in Construction"),
      CourseToProject("Customer Service", "ETE Portal - Introduction to Customer Service"),
      CourseToProject("Introduction to Digital", "ETE Portal - Introduction to Digital"),
      CourseToProject("Introduction to Electrical", "ETE Portal - Introduction to Electrical"),
      CourseToProject("Gardening and Horticulture", "ETE Portal - Introduction to Gardening & Horticulture"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "ETE Portal - Introduction to Industrial & Commercial Cleaning"),
      CourseToProject("Introduction to Plumbing", "ETE Portal - Introduction to Plumbing"),
      CourseToProject("Introduction to Retail", "ETE Portal - Introduction to Retail"),
      CourseToProject("Introduction to Self-Employment", "ETE Portal - Introduction to Self-Employment"),
      CourseToProject("Mental Health Awareness", "ETE Portal - Mental Health Awareness"),
      CourseToProject("Money Management", "ETE Portal - Money Management"),
      CourseToProject("Painting and Decorating", "ETE Portal - Painting & Decorating"),
      CourseToProject("First Aid", "ETE Portal – First Aid"),
      CourseToProject("Health and Safety", "ETE Portal – Health & Safety"),
      CourseToProject("Manual Handling", "ETE Portal – Manual Handling"),
    )

    private val greaterManchester = listOf(
      CourseToProject(null, "Alison - Community Campus"),
      CourseToProject("Adult Social Care", "Adult Social Care - Community Campus"),
      CourseToProject("Building your Path", "Building your Path - Community Campus"),
      CourseToProject("Health and Safety in Construction", "Construction Health & Safety - Community Campus"),
      CourseToProject("Effective Communication", "Effective Communication - Community Campus"),
      CourseToProject("Employability", "Employability - Community Campus"),
      CourseToProject("First Aid", "FIRST AID - Community Campus"),
      CourseToProject("Food Hygiene", "Food Hygiene - Community Campus"),
      CourseToProject("Health and Safety", "HEALTH & SAFETY - Community Campus"),
      CourseToProject("Customer Service", "Intro to Customer Service - Community Campus"),
      CourseToProject("Introduction to Digital", "Intro to Digital - Community Campus"),
      CourseToProject("Introduction to Electrical", "Intro to Electrical - Community Campus"),
      CourseToProject("Gardening and Horticulture", "Intro to Gardening & Horticulture - Community Campus"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "Intro to Industrial & Commercial Cleaning - Community Campus"),
      CourseToProject("Painting and Decorating", "Intro to Painting & Decorating - Community Campus"),
      CourseToProject("Introduction to Plumbing", "Intro to Plumbing - Community Campus"),
      CourseToProject("Introduction to Retail", "Intro to Retail - Community Campus"),
      CourseToProject("Introduction to Self-Employment", "Intro to Self-Employment - Community Campus"),
      CourseToProject("Manual Handling", "MANUAL HANDLING - Community Campus"),
      CourseToProject("Mental Health Awareness", "Mental Health Awareness - Community Campus"),
      CourseToProject("Money Management", "Money Management - Community Campus"),
    )

    private val kentSurreyAndSussex = listOf(
      CourseToProject("Adult Social Care", "CC Adult Social Care"),
      CourseToProject("Building your path: A Career in Construction", "CC Building your path: A Career in Construction"),
      CourseToProject("Effective Communication", "CC Effective Communication"),
      CourseToProject("Employability", "CC Employability"),
      CourseToProject(null, "CC Employability V2"),
      CourseToProject("First Aid", "CC First Aid"),
      CourseToProject("Food Hygiene", "CC Food Hygiene"),
      CourseToProject("Health and Safety", "CC Health & Safety"),
      CourseToProject("Health and Safety in Construction", "CC Health & Safety in Construction"),
      CourseToProject("Customer Service", "CC Introduction to Customer Service"),
      CourseToProject("Introduction to Digital", "CC Introduction to Digital"),
      CourseToProject("Introduction to Electrical", "CC Introduction to Electrical"),
      CourseToProject("Gardening and Horticulture", "CC Introduction to Garden & Horticulture"),
      CourseToProject("Introduction to Plumbing", "CC Introduction to Plumbing"),
      CourseToProject("Introduction to Retail", "CC Introduction To Retail"),
      CourseToProject("Introduction to Self-Employment", "CC Introduction to Self-Employment"),
      CourseToProject("Manual Handling", "CC Manual Handling"),
      CourseToProject("Mental Health Awareness", "CC Mental Health awareness"),
      CourseToProject("Money Management", "CC Money Management"),
      CourseToProject("Painting and Decorating", "CC Painting & Decorating"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "CC Introduction to Industrial & Commercial Cleaning"),
    )

    private val northEast = listOf(
      CourseToProject(null, "ETE - HMPPS Portal - Alison.com"),
      CourseToProject(null, "ETE - HMPPS Portal - Instruction"),
      CourseToProject(null, "ETE - HMPPS Portal Course, Health & Safety in Construction"),
      CourseToProject("Building your path: A Career in Construction", "ETE - HMPPS Portal, Building your path: A Career in Construction"),
      CourseToProject("Effective Communication", "ETE - HMPPS Portal, Effective Communication"),
      CourseToProject("Employability", "ETE - HMPPS Portal, Employability"),
      CourseToProject("First Aid", "ETE - HMPPS Portal, First Aid"),
      CourseToProject("Food Hygiene", "ETE - HMPPS Portal, Food Hygiene"),
      CourseToProject("Health and Safety", "ETE - HMPPS Portal, Health & Safety Course"),
      CourseToProject("Health and Safety in Construction", "ETE - HMPPS Portal, Health & Safety in Construction"),
      CourseToProject("Adult Social Care", "ETE - HMPPS Portal, Intro to Adult Social Care"),
      CourseToProject(null, "ETE - HMPPS Portal, Intro to Construction"),
      CourseToProject("Customer Service", "ETE - HMPPS Portal, Intro to Customer Service"),
      CourseToProject("Introduction to Digital", "ETE - HMPPS Portal, Intro to Digital"),
      CourseToProject("Introduction to Electrical", "ETE - HMPPS Portal, Intro to Electrical"),
      CourseToProject("Gardening and Horticulture", "ETE - HMPPS Portal, Intro to Gardening & Horticulture"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "ETE - HMPPS Portal, Intro to Industrial Cleaning"),
      CourseToProject("Introduction to Plumbing", "ETE - HMPPS Portal, Intro to Plumbing"),
      CourseToProject("Introduction to Retail", "ETE - HMPPS Portal, Intro to Retail"),
      CourseToProject("Introduction to Self-Employment", "ETE - HMPPS Portal, Intro to Self-Employment"),
      CourseToProject("Manual Handling", "ETE - HMPPS Portal, Manual Handling"),
      CourseToProject("Mental Health Awareness", "ETE - HMPPS Portal, Mental Health Awareness"),
      CourseToProject("Money Management", "ETE - HMPPS Portal, Money Management"),
      CourseToProject("Painting and Decorating", "ETE - HMPPS Portal, Painting & Decorating"),
    )

    private val northWest = listOf(
      NorthWestCase("N51CBH", projectName = "Preston ETE"),
      NorthWestCase("N51CAH", projectName = "Barrow ETE"),
      NorthWestCase("N51CAJ", projectName = "Blackburn ETE"),
      NorthWestCase("N51CAO", projectName = "Burnley ETE"),
      NorthWestCase("N51CAP", projectName = "Carlisle ETE"),
      NorthWestCase("N51LNP", projectName = "ETE Community Portal"),
      NorthWestCase("N51CWP", office = "Winsford Office", projectName = "ETE Community Portal Winsford"),
      NorthWestCase("N51CWP", office = "Chester Office", projectName = "ETE Community Portal Ellesmere Port/Chester"),
      NorthWestCase("N51CAV", projectName = "Chorley ETE"),
      NorthWestCase("N51CAK", projectName = "Blackpool ETE"),
      NorthWestCase("N51CEP", office = "Crewe Office", projectName = "ETE Community Portal Crewe"),
      NorthWestCase("N51CEP", office = "Macclesfield Office", projectName = "ETE Community Portal Macclesfield"),
      NorthWestCase("N51CAB", projectName = "Accrington ETE"),
      NorthWestCase("N51CBB", projectName = "Kendal ETE"),
      NorthWestCase("N51CBD", projectName = "Lancaster ETE"),
      NorthWestCase("N51KCP", projectName = "ETE Community Portal"),
      NorthWestCase("N51HWP", projectName = "ETE Community Portal"),
      NorthWestCase("N51CBM", projectName = "Skelmersdale ETE"),
      NorthWestCase("N51SCP", projectName = "ETE Community Portal"),
      NorthWestCase("N51WCP", projectName = "ETE Community Portal"),
      NorthWestCase("N51CBP", projectName = "Workington ETE"),
    )

    private val southCentral = listOf(
      CourseToProject(null, "ET5 ETE – HMPPS portal"),
      CourseToProject("First Aid", "ET5-ETE CC First Aid"),
      CourseToProject("Health and Safety", "ET5-ETE CC Health & Safety"),
      CourseToProject("Manual Handling", "ET5-ETE CC Manual Handling"),
    )

    private val southWest = listOf(
      CourseToProject(null, "ETE CC Alison Bath"),
      CourseToProject("Building your path: A Career in Construction", "ETE CC Building your path: A Career in Construction Bath"),
      CourseToProject("Effective Communication", "ETE CC Effective Communication Bath"),
      CourseToProject("Employability", "ETE CC Employability Bath"),
      CourseToProject("First Aid", "ETE CC First Aid Bath"),
      CourseToProject("Food Hygiene", "ETE CC Food Hygiene Bath"),
      CourseToProject("Health and Safety", "ETE CC Health and Safety Bath"),
      CourseToProject("Health and Safety in Construction", "ETE CC Health and Safety in Construction Bath"),
      CourseToProject("Adult Social Care", "ETE CC Intro to Adult Social Care Bath"),
      CourseToProject("Customer Service", "ETE CC Intro to Customer Services Bath"),
      CourseToProject("Introduction to Digital", "ETE CC Intro to Digital (IT) Bath"),
      CourseToProject("Introduction to Electrical", "ETE CC Intro to Electrical Bath"),
      CourseToProject("Gardening and Horticulture", "ETE CC Intro to Gardening & Horticulture Bath"),
      CourseToProject("Introduction to Industrial and Commercial Cleaning", "ETE CC Intro to Industrial & Commercial Cleaning Bath"),
      CourseToProject("Introduction to Plumbing", "ETE CC Intro to Plumbing Bath"),
      CourseToProject("Introduction to Retail", "ETE CC Intro to Retail Bath"),
      CourseToProject("Introduction to Self-Employment", "ETE CC Intro to Self Employment Bath"),
      CourseToProject(null, "ETE CC Mandated Courses Bath"),
      CourseToProject("Manual Handling", "ETE CC Manual Handling Bath"),
      CourseToProject("Mental Health Awareness", "ETE CC Mental Health Awareness Bath"),
      CourseToProject("Money Management", "ETE CC Money Management Bath"),
      CourseToProject("Painting and Decorating", "ETE CC Painting & Decorating Bath"),
    )

    private val yorksAndHumber = listOf(
      CourseToProject("Employability", "ETE HMPPS Portal - Employability - mandatory if unemployed"),
      CourseToProject("First Aid", "ETE HMPPS Portal - First Aid - mandatory"),
      CourseToProject("Health and Safety", "ETE HMPPS Portal - Health and Safety - mandatory"),
      CourseToProject("Manual Handling", "ETE HMPPS Portal - Manual Handling - mandatory"),
      CourseToProject(null, "ET5 ETE – HMPPS Portal - Other"),
      CourseToProject(null, "ETE Alison.com Course"),
    )
  }
}

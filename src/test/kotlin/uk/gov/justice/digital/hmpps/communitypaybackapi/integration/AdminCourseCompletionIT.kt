package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.hateoas.PagedModel
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseDetail
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCode
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionRecommendationDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.CommunityCampusPduEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionResolution
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.unallocated
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.failed
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.passed
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.SchedulingIT.Companion.EVENT_NUMBER
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DatabasePurgeUtils
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer.ExpectedAppointmentCreate
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AdminCourseCompletionIT : IntegrationTestBase() {

  @Autowired
  lateinit var communityCampusPduEntityRepository: CommunityCampusPduEntityRepository

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var eteCourseCompletionEventResolutionRepository: EteCourseCompletionEventResolutionRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @Autowired
  lateinit var databasePurgeUtils: DatabasePurgeUtils

  companion object {
    const val CRN = "X12345"
    const val DELIUS_EVENT_NUMBER = 5L
    const val PROJECT_CODE = "PRJ001"
  }

  @Nested
  @DisplayName("GET /providers/N07/course-completions")
  inner class GetCourseCompletionsEndpoint {

    @BeforeEach
    fun setUp() {
      databasePurgeUtils.deleteAllEteData()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/providers/N07/course-completions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/providers/N07/course-completions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/providers/N07/course-completions")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return results for requested provider only`() {
      val walesProviderEvent1 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = communityCampusPduEntityRepository.findByNameIgnoreCase("Dyfed Powys")!!,
        ),
      )

      val walesProviderEvent2 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = communityCampusPduEntityRepository.findByNameIgnoreCase("Cwm Taf Morgannwg")!!,
        ),
      )

      // london
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = communityCampusPduEntityRepository.findByNameIgnoreCase("Enfield and Haringey")!!,
        ),
      )

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/N03/course-completions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content.map { it.id }).containsExactlyInAnyOrder(walesProviderEvent1.id, walesProviderEvent2.id)
    }

    @Test
    fun `should return results for requested pdu only`() {
      val walesPdu1 = communityCampusPduEntityRepository.findByNameIgnoreCase("Dyfed Powys")!!
      val walesPdu2 = communityCampusPduEntityRepository.findByNameIgnoreCase("Cwm Taf Morgannwg")!!

      val walesPdu1Completion1 = eteCourseCompletionEventEntityRepository.save(EteCourseCompletionEventEntity.passed(ctx).copy(pdu = walesPdu1))
      val walesPdu1Completion2 = eteCourseCompletionEventEntityRepository.save(EteCourseCompletionEventEntity.passed(ctx).copy(pdu = walesPdu1))
      eteCourseCompletionEventEntityRepository.save(EteCourseCompletionEventEntity.passed(ctx).copy(pdu = walesPdu2))

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/N03/course-completions?pduId=${walesPdu1.id}")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content.map { it.id }).containsExactlyInAnyOrder(walesPdu1Completion1.id, walesPdu1Completion2.id)
    }

    @Test
    fun `should return OK for one course completion`() {
      val entity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx),
      )

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/${entity.pdu.providerCode}/course-completions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content).hasSize(1)
      assertThat(pagedCourseCompletions.content.first().id).isEqualTo(entity.id)
    }

    @Test
    fun `should exclude failed completions`() {
      val pdu = communityCampusPduEntityRepository.findByNameIgnoreCase("Dyfed Powys")!!

      val completionPassed = eteCourseCompletionEventEntityRepository.save(EteCourseCompletionEventEntity.passed(ctx).copy(pdu = pdu))
      eteCourseCompletionEventEntityRepository.save(EteCourseCompletionEventEntity.failed(ctx).copy(pdu = pdu))

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content.map { it.id }).containsExactly(completionPassed.id)
    }

    @Test
    fun `should apply date range`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      // before range
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          completionDate = LocalDate.of(2025, 5, 9),
        ),
      )

      val inRange1 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          completionDate = LocalDate.of(2025, 5, 10),
        ),
      )

      val inRange2 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          completionDate = LocalDate.of(2025, 6, 20),
        ),
      )

      // after range
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          completionDate = LocalDate.of(2025, 6, 21),
        ),
      )

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?dateFrom=2025-05-10&dateTo=2025-06-20&sort=completionDate,asc")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content).hasSize(2)
      assertThat(pagedCourseCompletions.content.first().id).isEqualTo(inRange1.id)
      assertThat(pagedCourseCompletions.content.last().id).isEqualTo(inRange2.id)
    }

    @Test
    fun `should return resolved and unresolved if no resolution filter defined`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      val resolved = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )
      eteCourseCompletionEventResolutionRepository.save(
        EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
          eteCourseCompletionEvent = resolved,
        ),
      )

      val unresolved = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )

      val result = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(result.content.map { it.id }).containsExactlyInAnyOrder(resolved.id, unresolved.id)
    }

    @Test
    fun `should only return resolved if resolved requested`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      val resolved = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )

      eteCourseCompletionEventResolutionRepository.save(
        EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
          eteCourseCompletionEvent = resolved,
        ),
      )

      // unresolved
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )

      val result = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?resolutionStatus=Resolved")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(result.content.map { it.id }).containsExactlyInAnyOrder(resolved.id)
    }

    @Test
    fun `should only return unresolved if unresolved requested`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      val resolved = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )

      eteCourseCompletionEventResolutionRepository.save(
        EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
          eteCourseCompletionEvent = resolved,
        ),
      )

      val unresolved = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
        ),
      )

      val result = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?resolutionStatus=Unresolved")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(result.content.map { it.id }).containsExactlyInAnyOrder(unresolved.id)
    }

    @Test
    fun `should apply office filter`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          office = "Hammersmith",
        ),
      )

      val inOffice = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          office = "Whitechapel",
        ),
      )

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?office=Whitechapel")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content).hasSize(1)
      assertThat(pagedCourseCompletions.content.first().id).isEqualTo(inOffice.id)
    }

    @Test
    fun `should apply multiple office filters`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          office = "Hammersmith",
        ),
      )

      val inOffice1 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          office = "Whitechapel",
        ),
      )

      val inOffice2 = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          pdu = pdu,
          office = "Croydon",
        ),
      )

      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?office=Whitechapel&office=Croydon")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletions.content.map { it.id }).containsExactlyInAnyOrder(
        inOffice1.id,
        inOffice2.id,
      )
    }

    @Test
    fun `should return OK for multiple course completions with pagination`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      repeat(10) {
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.passed(ctx).copy(
            pdu = pdu,
          ),
        )
      }

      val pagedCourseCompletionsPage1 = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?page=0&size=5")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletionsPage1.metadata?.totalPages).isEqualTo(2)
      assertThat(pagedCourseCompletionsPage1.metadata?.size).isEqualTo(5)
      assertThat(pagedCourseCompletionsPage1.metadata?.number).isEqualTo(0)
      assertThat(pagedCourseCompletionsPage1.content).hasSize(5)

      val pagedCourseCompletionsPage2 = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?page=1&size=5")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletionsPage2.metadata?.totalPages).isEqualTo(2)
      assertThat(pagedCourseCompletionsPage2.metadata?.size).isEqualTo(5)
      assertThat(pagedCourseCompletionsPage2.metadata?.number).isEqualTo(1)
      assertThat(pagedCourseCompletionsPage2.content).hasSize(5)
    }

    @Test
    fun `should return OK for multiple course completions with sorting`() {
      val pdu = communityCampusPduEntityRepository.findAll().first()

      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          firstName = "John",
          lastName = "Smith",
          pdu = pdu,
        ),
      )
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          firstName = "John",
          lastName = "Doe",
          pdu = pdu,
        ),
      )
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          firstName = "Pi",
          lastName = "Patel",
          pdu = pdu,
        ),
      )
      eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.passed(ctx).copy(
          firstName = "Zack",
          lastName = "Jones",
          pdu = pdu,
        ),
      )

      val pagedCourseCompletionsPage = webTestClient.get()
        .uri("/admin/providers/${pdu.providerCode}/course-completions?&sort=firstName,lastName,desc")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

      assertThat(pagedCourseCompletionsPage.content).hasSize(4)
      val contentList = pagedCourseCompletionsPage.content.toList()
      assertThat(contentList[0].firstName).isEqualTo("Zack")
      assertThat(contentList[0].lastName).isEqualTo("Jones")
      assertThat(contentList[1].firstName).isEqualTo("Pi")
      assertThat(contentList[1].lastName).isEqualTo("Patel")
      assertThat(contentList[2].firstName).isEqualTo("John")
      assertThat(contentList[2].lastName).isEqualTo("Smith")
      assertThat(contentList[3].firstName).isEqualTo("John")
      assertThat(contentList[3].lastName).isEqualTo("Doe")
    }
  }

  @Nested
  @DisplayName("POST /course-completion/{eteCourseCompletionEventId}/resolution")
  inner class PostCourseCompletionResolution {

    @BeforeEach
    fun setUp() {
      databasePurgeUtils.deleteAllEteData()
    }

    @BeforeEach
    fun setupCommonWiremocks() {
      val project = NDProject.valid(ctx).copy(code = PROJECT_CODE, actualEndDateExclusive = null)
      CommunityPaybackAndDeliusMockServer.getProject(project)
      CommunityPaybackAndDeliusMockServer.getTeamSupervisors(
        forProject = project,
        supervisorSummaries = NDSupervisorSummaries(listOf(NDSupervisorSummary.unallocated())),
      )
    }

    @Test
    fun `should return unauthorized if no token`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completion/$id/resolution")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CourseCompletionResolutionDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completions/$id/resolution")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CourseCompletionResolutionDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completions/$id/resolution")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CourseCompletionResolutionDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 if course completion not found`() {
      webTestClient.post()
        .uri("/admin/course-completions/${UUID.randomUUID()}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CourseCompletionResolutionDto.valid(ctx))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should error if validation fails`() {
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(),
      )

      val resolution = CourseCompletionResolutionDto.valid().copy(
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid().copy(
          contactOutcomeCode = "WRONG",
        ),
      )

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `if type is COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD, just record resolution`() {
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx),
      )

      val resolutionDto = CourseCompletionResolutionDto.valid(ctx).copy(
        type = CourseCompletionResolutionTypeDto.COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD,
        crn = CRN,
        creditTimeDetails = null,
      )

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolutionDto)
        .exchange()
        .expectStatus()
        .isNoContent

      val resolutionEntity = eteCourseCompletionEventEntityRepository.findByIdOrNull(eventEntity.id)!!.resolution!!
      assertThat(resolutionEntity.resolution == EteCourseCompletionResolution.COURSE_ALREADY_COMPLETED_WITHIN_THRESHOLD)
    }

    @Test
    fun `if type is CREDIT_TIME, should create appointment when appointmentIdToUpdate is null`() {
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(),
      )

      val resolution = CourseCompletionResolutionDto.valid(ctx).copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        crn = CRN,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx).copy(
          date = LocalDate.of(2021, 1, 30),
          deliusEventNumber = DELIUS_EVENT_NUMBER,
          appointmentIdToUpdate = null,
          projectCode = PROJECT_CODE,
          minutesToCredit = 90,
        ),
      )

      val project = NDProject.valid(ctx).copy(code = PROJECT_CODE, actualEndDateExclusive = null)
      CommunityPaybackAndDeliusMockServer.getProject(project)
      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = CRN,
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(
            eventNumber = DELIUS_EVENT_NUMBER,
            sentenceDate = LocalDate.of(2021, 1, 10),
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.getTeamSupervisors(
        forProject = project,
        supervisorSummaries = NDSupervisorSummaries(listOf(NDSupervisorSummary.unallocated())),
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(projectCode = PROJECT_CODE, appointmentCount = 1)

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isNoContent

      val expectedAppointment = ExpectedAppointmentCreate(
        crn = CRN,
        eventNumber = DELIUS_EVENT_NUMBER,
        date = LocalDate.of(2021, 1, 30),
        startTime = LocalTime.of(0, 0),
        endTime = LocalTime.of(1, 30),
      )

      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = PROJECT_CODE,
        expectedAppointments = listOf(expectedAppointment),
      )

      assertThat(eteCourseCompletionEventEntityRepository.findByIdOrNull(eventEntity.id)!!.resolution).isNotNull
    }

    @Test
    fun `if type is CREDIT_TIME, should update appointment when appointmentIdToUpdate is present`() {
      val appointmentId = 12345L

      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(),
      )
      val resolution = CourseCompletionResolutionDto.valid(ctx).copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        crn = CRN,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx).copy(
          date = LocalDate.now().minusDays(5),
          appointmentIdToUpdate = appointmentId,
          projectCode = PROJECT_CODE,
        ),
      )

      val upstreamAppointment = NDAppointment.validNoOutcome(ctx).copy(
        id = appointmentId,
        project = NDProjectAndLocation.valid().copy(
          code = PROJECT_CODE,
        ),
        date = LocalDate.now().minusDays(5),
        event = NDEvent.valid().copy(number = EVENT_NUMBER.toInt()),
        case = NDCaseSummary.valid().copy(crn = SchedulingIT.CRN),
      )
      CommunityPaybackAndDeliusMockServer.getAppointment(
        appointment = upstreamAppointment,
        username = "theusername",
      )
      CommunityPaybackAndDeliusMockServer.getProject(NDProject.valid(ctx).copy(code = "proj123"))
      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = SchedulingIT.CRN,
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(
            eventNumber = EVENT_NUMBER,
            sentenceDate = LocalDate.now().minusYears(1),
          ),
        ),
      )

      CommunityPaybackAndDeliusMockServer.putAppointment(
        projectCode = PROJECT_CODE,
        appointmentId = appointmentId,
      )

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isNoContent

      CommunityPaybackAndDeliusMockServer.putAppointmentVerify(
        projectCode = PROJECT_CODE,
        appointmentId = appointmentId,
      )

      assertThat(eteCourseCompletionEventEntityRepository.findByIdOrNull(eventEntity.id)!!.resolution).isNotNull
    }

    @Test
    fun `should return success but do nothing if an identical resolution has already been applied`() {
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          completionDate = LocalDate.now().minusDays(1),
        ),
      )

      val resolution = CourseCompletionResolutionDto.valid(ctx).copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        crn = CRN,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx).copy(
          deliusEventNumber = DELIUS_EVENT_NUMBER,
          appointmentIdToUpdate = null,
          projectCode = PROJECT_CODE,
          minutesToCredit = 90,
        ),
      )

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = CRN,
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(
            eventNumber = DELIUS_EVENT_NUMBER,
            sentenceDate = LocalDate.now().minusDays(10),
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(projectCode = PROJECT_CODE, appointmentCount = 1)

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isNoContent

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isNoContent

      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = PROJECT_CODE,
        totalExpectedCalls = 1,
      )

      assertThat(eteCourseCompletionEventEntityRepository.findByIdOrNull(eventEntity.id)!!.resolution).isNotNull
    }

    @Test
    fun `should error if a differing resolution has already been applied`() {
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          completionDate = LocalDate.now().minusDays(1),
        ),
      )

      val resolution = CourseCompletionResolutionDto.valid(ctx).copy(
        type = CourseCompletionResolutionTypeDto.CREDIT_TIME,
        crn = CRN,
        creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx).copy(
          deliusEventNumber = DELIUS_EVENT_NUMBER,
          appointmentIdToUpdate = null,
          projectCode = PROJECT_CODE,
          minutesToCredit = 90,
        ),
      )

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(
        crn = CRN,
        unpaidWorkDetails = listOf(
          NDCaseDetail.valid().copy(
            eventNumber = DELIUS_EVENT_NUMBER,
            sentenceDate = LocalDate.now().minusDays(10),
          ),
        ),
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(projectCode = PROJECT_CODE, appointmentCount = 1)

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(resolution)
        .exchange()
        .expectStatus()
        .isNoContent

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}/resolution")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          resolution.copy(
            creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx).copy(
              minutesToCredit = resolution.creditTimeDetails!!.minutesToCredit + 1,
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage").isEqualTo("Validation failure: A resolution has already been defined for this course completion record")
    }
  }

  @Nested
  @DisplayName("GET /course-completions/{id}")
  inner class GetCourseCompletionEndpoint {

    val id: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      eteCourseCompletionEventEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/course-completions/$id")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/course-completions/$id")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/course-completions/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 when course completion not found`() {
      webTestClient.get()
        .uri("/admin/course-completions/$id")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return OK for a course completion`() {
      val entity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          region = "London",
        ),
      )

      val courseCompletionEvent = webTestClient.get()
        .uri("/admin/course-completions/${entity.id}")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<EteCourseCompletionEventDto>()

      assertThat(courseCompletionEvent.id).isEqualTo(entity.id)
      assertThat(courseCompletionEvent.firstName).isEqualTo(entity.firstName)
    }
  }

  @Nested
  @DisplayName("GET /course-completions/{id}/recommended-selection")
  open inner class GetCourseCompletionRecommendationEndpoint {

    @BeforeEach
    fun setUp() {
      databasePurgeUtils.deleteAllEteData()
    }

    @Test
    fun `should return unauthorized if no token`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/admin/course-completions/$id/recommended-selection")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/admin/course-completions/$id/recommended-selection")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 when course completion not found`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/admin/course-completions/$id/recommended-selection")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return recommendation with all fields when available`() {
      val email = "matched@example.com"
      val eventWithCrn = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          email = email,
        ),
      )
      val crn = "X123456"
      eteCourseCompletionEventResolutionRepository.save(
        EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
          eteCourseCompletionEvent = eventWithCrn,
          crn = crn,
        ),
      )

      val office = "Office A"
      val courseName = "Course B"
      val projectCode = "PRJ001"
      val eventWithProject = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          office = office,
          courseName = courseName,
        ),
      )
      eteCourseCompletionEventResolutionRepository.save(
        EteCourseCompletionEventResolutionEntity.valid(ctx).copy(
          eteCourseCompletionEvent = eventWithProject,
          projectCode = projectCode,
        ),
      )

      val targetEvent = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx).copy(
          email = email,
          office = office,
          courseName = courseName,
        ),
      )

      val teamCode = "TEAM1"
      val project = NDProject.valid(ctx).copy(
        code = projectCode,
        team = NDCode(teamCode),
      )
      CommunityPaybackAndDeliusMockServer.getProject(project)

      val recommendation = webTestClient.get()
        .uri("/admin/course-completions/${targetEvent.id}/recommended-selection")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CourseCompletionRecommendationDto>()

      assertThat(recommendation.crn).isEqualTo(crn)
      assertThat(recommendation.project?.projectCode).isEqualTo(projectCode)
      assertThat(recommendation.project?.teamCode).isEqualTo(teamCode)
    }

    @Test
    fun `should return recommendation with nulls when no previous resolutions exist`() {
      val targetEvent = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(ctx),
      )

      val recommendation = webTestClient.get()
        .uri("/admin/course-completions/${targetEvent.id}/recommended-selection")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<CourseCompletionRecommendationDto>()

      assertThat(recommendation.crn).isNull()
      assertThat(recommendation.project?.projectCode).isNull()
      assertThat(recommendation.project?.teamCode).isNull()
    }
  }
}

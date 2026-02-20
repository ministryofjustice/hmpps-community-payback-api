package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import io.mockk.impl.annotations.RelaxedMockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.PagedModel
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDSupervisorSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.unallocated
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer.ExpectedAppointmentCreate
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.ContextService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AdminCourseCompletionIT : IntegrationTestBase() {

  @Autowired
  lateinit var eteCourseCompletionEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @RelaxedMockK
  lateinit var contextService: ContextService

  @Nested
  @DisplayName("GET /providers/N07/course-completions")
  inner class CourseCompletionsEndpoint {

    @Nested
    @DisplayName("GET /providers/N07/course-completions")
    inner class CourseCompletionsEndpoint {

      @BeforeEach
      fun setUp() {
        eteCourseCompletionEventEntityRepository.deleteAll()
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
      fun `should return empty results for invalid provider code`() {
        val pagedCourseCompletions = webTestClient.get()
          .uri("/admin/providers/INVALID/course-completions")
          .addAdminUiAuthHeader()
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

        assertThat(pagedCourseCompletions.content).isEmpty()
      }

      @Test
      fun `should return OK for one course completion`() {
        val entity = eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
          ),
        )

        val pagedCourseCompletions = webTestClient.get()
          .uri("/admin/providers/N07/course-completions")
          .addAdminUiAuthHeader()
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<PagedModel<EteCourseCompletionEventDto>>()

        assertThat(pagedCourseCompletions.content).hasSize(1)
        assertThat(pagedCourseCompletions.content.first().id).isEqualTo(entity.id)
      }

      @Test
      fun `should apply date range`() {
        // before range
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 5, 9),
          ),
        )

        val inRange1 = eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 5, 10),
          ),
        )

        val inRange2 = eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 6, 20),
          ),
        )

        // after range
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 6, 21),
          ),
        )

        val pagedCourseCompletions = webTestClient.get()
          .uri("/admin/providers/N07/course-completions?dateFrom=2025-05-10&dateTo=2025-06-20&sort=completionDate,asc")
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
      fun `should return OK for multiple course completions with pagination`() {
        repeat(10) {
          eteCourseCompletionEventEntityRepository.save(
            EteCourseCompletionEventEntity.valid().copy(
              region = "London",
            ),
          )
        }

        val pagedCourseCompletionsPage1 = webTestClient.get()
          .uri("/admin/providers/N07/course-completions?page=0&size=5")
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
          .uri("/admin/providers/N07/course-completions?page=1&size=5")
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
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "John",
            lastName = "Smith",
            region = "London",
          ),
        )
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "John",
            lastName = "Doe",
            region = "London",
          ),
        )
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "Pi",
            lastName = "Patel",
            region = "London",
          ),
        )
        eteCourseCompletionEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "Zack",
            lastName = "Jones",
            region = "London",
          ),
        )

        val pagedCourseCompletionsPage = webTestClient.get()
          .uri("/admin/providers/N07/course-completions?&sort=firstName,lastName,desc")
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
  }

  @Nested
  @DisplayName("POST /course-completion/{eteCourseCompletionEventId}")
  inner class PostCourseCompletionOutcomeEndpoint {

    val courseCompletionOutcomeDto = CourseCompletionOutcomeDto(
      crn = "X123456",
      appointmentIdToUpdate = null,
      minutesToCredit = 60,
      contactOutcome = "COMP",
      projectCode = "PRJ001",
    )

    @BeforeEach
    fun setUp() {
      eteCourseCompletionEventEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completion/$id")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(courseCompletionOutcomeDto)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completions/$id")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(courseCompletionOutcomeDto)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completions/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(courseCompletionOutcomeDto)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should create appointment when appointmentIdToUpdate is null`() {
      val projectCode = "PRJ001"
      val crn = "X999999"
      val minutesToCredit = 90L
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(),
      )
      val outcome = CourseCompletionOutcomeDto.valid().copy(
        crn = crn,
        appointmentIdToUpdate = null,
        minutesToCredit = minutesToCredit,
        contactOutcome = "ATTC",
        projectCode = projectCode,
      )

      val project = NDProject.valid(ctx).copy(code = projectCode, actualEndDateExclusive = null)
      CommunityPaybackAndDeliusMockServer.getProject(project)
      CommunityPaybackAndDeliusMockServer.getTeamSupervisors(
        forProject = project,
        supervisorSummaries = NDSupervisorSummaries(listOf(NDSupervisorSummary.unallocated())),
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(projectCode = projectCode, appointmentCount = 1)

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(outcome)
        .exchange()
        .expectStatus()
        .isNoContent

      val expectedAppointment = ExpectedAppointmentCreate(
        crn = crn,
        eventNumber = 1,
        date = eventEntity.completionDate,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 0).plusMinutes(minutesToCredit),
      )

      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = projectCode,
        expectedAppointments = listOf(expectedAppointment),
      )
    }

    @Test
    fun `should update appointment when appointmentIdToUpdate is present`() {
      val projectCode = "PRJ001"
      val crn = "X999999"
      val minutesToCredit = 90L
      val appointmentId = 12345L
      val eventEntity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid(),
      )
      val outcome = CourseCompletionOutcomeDto.valid().copy(
        crn = crn,
        appointmentIdToUpdate = null,
        minutesToCredit = minutesToCredit,
        contactOutcome = "ATTC",
        projectCode = projectCode,
      )

      val upstreamAppointment = NDAppointment.validNoOutcome(ctx).copy(
        id = appointmentId,
        project = NDProjectAndLocation.valid().copy(
          code = projectCode,
        ),
        date = LocalDate.now().minusDays(5),
      )
      CommunityPaybackAndDeliusMockServer.getAppointment(
        appointment = upstreamAppointment,
        username = "theusername",
      )
      val project = NDProject.valid(ctx).copy(code = projectCode, actualEndDateExclusive = null)
      CommunityPaybackAndDeliusMockServer.getProject(project)
      CommunityPaybackAndDeliusMockServer.getTeamSupervisors(
        forProject = project,
        supervisorSummaries = NDSupervisorSummaries(listOf(NDSupervisorSummary.unallocated())),
      )
      CommunityPaybackAndDeliusMockServer.putAppointment(
        projectCode = projectCode,
        appointmentId = appointmentId,
      )
      CommunityPaybackAndDeliusMockServer.postAppointments(
        projectCode = projectCode,
        appointmentCount = 1,
      )

      webTestClient.post()
        .uri("/admin/course-completions/${eventEntity.id}")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(outcome)
        .exchange()
        .expectStatus()
        .isNoContent

      val expectedAppointment = ExpectedAppointmentCreate(
        crn = crn,
        eventNumber = 1,
        date = eventEntity.completionDate,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 0).plusMinutes(minutesToCredit),
      )

      CommunityPaybackAndDeliusMockServer.postAppointmentVerify(
        projectCode = projectCode,
        expectedAppointments = listOf(expectedAppointment),
      )
    }

    @Test
    fun `should return 404 when ete event not found`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/admin/course-completion/$id")
        .addAdminUiAuthHeader("theusername")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          CourseCompletionOutcomeDto(
            crn = "X123456",
            appointmentIdToUpdate = null,
            minutesToCredit = 60,
            contactOutcome = "COMP",
            projectCode = "PRJ001",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /course-completions/{id}")
  inner class CourseCompletionEndpoint {

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
      val pagedCourseCompletions = webTestClient.get()
        .uri("/admin/course-completions/$id")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return OK for a course completion`() {
      val entity = eteCourseCompletionEventEntityRepository.save(
        EteCourseCompletionEventEntity.valid().copy(
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
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.hateoas.PagedModel
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CreateEteUserRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.EteCourseCompletionEventDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteUserRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate
import java.util.UUID

class AdminCourseCompletionIT : IntegrationTestBase() {

  @Autowired
  lateinit var eteAppointmentEventEntityRepository: EteCourseCompletionEventEntityRepository

  @Autowired
  lateinit var eteUserRepository: EteUserRepository

  @Nested
  @DisplayName("GET /providers/N07/course-completions")
  inner class CourseCompletionsEndpoint {

    @Nested
    @DisplayName("GET /providers/N07/course-completions")
    inner class CourseCompletionsEndpoint {

      @BeforeEach
      fun setUp() {
        eteAppointmentEventEntityRepository.deleteAll()
        eteUserRepository.deleteAll()
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
        val entity = eteAppointmentEventEntityRepository.save(
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
        eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 5, 9),
          ),
        )

        val inRange1 = eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 5, 10),
          ),
        )

        val inRange2 = eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            region = "London",
            completionDate = LocalDate.of(2025, 6, 20),
          ),
        )

        // after range
        eteAppointmentEventEntityRepository.save(
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
          eteAppointmentEventEntityRepository.save(
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
        eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "John",
            lastName = "Smith",
            region = "London",
          ),
        )
        eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "John",
            lastName = "Doe",
            region = "London",
          ),
        )
        eteAppointmentEventEntityRepository.save(
          EteCourseCompletionEventEntity.valid().copy(
            firstName = "Pi",
            lastName = "Patel",
            region = "London",
          ),
        )
        eteAppointmentEventEntityRepository.save(
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
  @DisplayName("GET /course-completions/{id}")
  inner class CourseCompletionEndpoint {

    val id: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      eteAppointmentEventEntityRepository.deleteAll()
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
      val entity = eteAppointmentEventEntityRepository.save(
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

  @Nested
  @DisplayName("POST /admin/ete-users")
  inner class CreateEteUser {

    @Test
    fun `should return 201 when user is created and 204 when it already exists`() {
      val request = CreateEteUserRequest(
        crn = "X123456",
        email = "test.user@digital.justice.gov.uk",
      )

      CommunityPaybackAndDeliusMockServer.getUpwDetailsSummary(request.crn)

      webTestClient.post()
        .uri("/admin/ete/users")
        .addAdminUiAuthHeader()
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isCreated

      webTestClient.post()
        .uri("/admin/ete/users")
        .addAdminUiAuthHeader()
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isNoContent
    }

    @Test
    fun `should return 400 bad request for invalid email`() {
      val invalidRequest = mapOf(
        "crn" to "X123456",
        "email" to "not-an-email",
      )

      webTestClient.post()
        .uri("/admin/ete/users")
        .addAdminUiAuthHeader()
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus()
        .isBadRequest
        .bodyAsObject<String>().contains("/Field error in object 'createEteUserRequest' on field 'email':/")
    }
  }
}

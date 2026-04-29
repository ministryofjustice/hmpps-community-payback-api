package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseAccessItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.excluded
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.excludedAndRestricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.restricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.unrestricted
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.persist
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity.validPending
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.ProbationAccessControlMockServer
import java.time.LocalDate
import java.util.UUID

class AdminAppointmentTaskIT : IntegrationTestBase() {

  private lateinit var fixtureFactory: FixtureFactory

  @Autowired
  lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @Autowired
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

  @BeforeEach
  fun setup() {
    ProbationAccessControlMockServer.setupGetAccessControlForCrnsDefault()
    fixtureFactory = FixtureFactory(ctx)
  }

  @AfterEach
  fun cleanup() {
    fixtureFactory.cleanup()
  }

  @Nested
  @DisplayName("GET /admin/appointment-tasks/pending")
  inner class GetAppointmentTasks {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/appointment-tasks/pending")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/appointment-tasks/pending")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/appointment-tasks/pending")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return empty page when no pending appointment tasks`() {
      val pageResponse = webTestClient.get()
        .uri("/admin/appointment-tasks/pending")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      assertThat(pageResponse.content).isEmpty()
      assertThat(pageResponse.page.totalElements).isEqualTo(0)
    }

    @Test
    fun `should return pending appointment tasks with default pagination`() {
      val projectType = fixtureFactory.projectType()
      val appointment = AppointmentEntity.valid().copy(
        deliusId = 101L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "thefirstname",
        lastName = "thelastname",
        projectType = projectType,
      ).persist(ctx)
      saveTask(appointment)

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "theusername",
        appointments = listOf(NDAppointmentSummary.forAppointment(appointment)),
        appointmentIds = listOf(101L),
        sortString = "name,desc",
        pageSize = 1,
      )

      val pageResponse = webTestClient.get()
        .uri("/admin/appointment-tasks/pending")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      assertThat(pageResponse.content).hasSize(1)
      assertThat(pageResponse.content[0].appointment.id).isEqualTo(101L)
      assertThat(pageResponse.page.size).isEqualTo(50)
      assertThat(pageResponse.content[0].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      val offender = pageResponse.content[0].offender as OffenderDto.OffenderFullDto
      assertThat(offender.crn).isEqualTo(appointment.crn)
      assertThat(offender.forename).isEqualTo("thefirstname")
      assertThat(offender.surname).isEqualTo("thelastname")
      assertThat(pageResponse.content[0].date).isEqualTo(appointment.date)
      assertThat(pageResponse.content[0].projectTypeName).isEqualTo(appointment.projectType!!.name)
    }

    @Test
    fun `should return pending appointment tasks with multiple pages`() {
      val appointment = AppointmentEntity.valid().copy(
        deliusId = 101L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
      ).persist(ctx)
      saveTask(appointment)

      val appointment2 = AppointmentEntity.valid().copy(
        deliusId = 102L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
      ).persist(ctx)
      saveTask(appointment2)

      val appointment3 = AppointmentEntity.valid().copy(
        deliusId = 103L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
      ).persist(ctx)
      saveTask(appointment3)

      val appointment4 = AppointmentEntity.valid().copy(
        deliusId = 104L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
      ).persist(ctx)
      saveTask(appointment4)

      val appointment5 = AppointmentEntity.valid().copy(
        deliusId = 105L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
      ).persist(ctx)
      saveTask(appointment5)

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "theusername",
        appointments = listOf(NDAppointmentSummary.forAppointment(appointment)),
        appointmentIds = listOf(101L),
        sortString = "name,desc",
        pageSize = 1,
      )

      val pageResponse = webTestClient.get()
        .uri("/admin/appointment-tasks/pending?page=2&size=2")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      assertThat(pageResponse.content).hasSize(1)
      assertThat(pageResponse.content[0].appointment.id).isEqualTo(101L)
      assertThat(pageResponse.page.size).isEqualTo(2)
      assertThat(pageResponse.page.totalPages).isEqualTo(3)
      assertThat(pageResponse.page.totalElements).isEqualTo(5)
      assertThat(pageResponse.page.number).isEqualTo(2)
    }

    @Test
    fun `should support sorting`() {
      val appointment1 = AppointmentEntity.valid().copy(
        deliusId = 101L,
        crn = "CRN111",
      ).persist(ctx)
      val task1 = saveTask(appointment1)

      val appointment2 = appointmentEntityRepository.save(
        AppointmentEntity.valid().copy(
          deliusId = 102L,
          crn = "CRN999",
        ),
      ).persist(ctx)
      val task2 = saveTask(appointment2)

      val appointment3 = appointmentEntityRepository.save(
        AppointmentEntity.valid().copy(
          deliusId = 103L,
          crn = "CRN222",
        ),
      ).persist(ctx)
      val task3 = saveTask(appointment3)

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "theusername",
        appointmentIds = listOf(
          appointment1.deliusId,
          appointment2.deliusId,
          appointment3.deliusId,
        ),
        appointments = listOf(
          NDAppointmentSummary.forAppointment(appointment1),
          NDAppointmentSummary.forAppointment(appointment2),
          NDAppointmentSummary.forAppointment(appointment3),
        ),
        sortString = "name,desc",
        pageSize = 3,
      )

      fun invokeEndpointWithSort(sort: String) = webTestClient.get()
        .uri("/admin/appointment-tasks/pending?page=0&size=10&sort=$sort")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      invokeEndpointWithSort("appointment.crn,asc").apply {
        assertThat(content).hasSize(3)
        assertThat(content[0].appointment.offender.crn).isEqualTo("CRN111")
        assertThat(content[1].appointment.offender.crn).isEqualTo("CRN222")
        assertThat(content[2].appointment.offender.crn).isEqualTo("CRN999")
      }

      invokeEndpointWithSort("appointment.crn,desc").apply {
        assertThat(content).hasSize(3)
        assertThat(content[0].appointment.offender.crn).isEqualTo("CRN999")
        assertThat(content[1].appointment.offender.crn).isEqualTo("CRN222")
        assertThat(content[2].appointment.offender.crn).isEqualTo("CRN111")
      }

      invokeEndpointWithSort("createdAt,asc").apply {
        assertThat(content).hasSize(3)
        assertThat(content[0].taskId).isEqualTo(task1.id)
        assertThat(content[1].taskId).isEqualTo(task2.id)
        assertThat(content[2].taskId).isEqualTo(task3.id)
      }

      invokeEndpointWithSort("createdAt,desc").apply {
        assertThat(content).hasSize(3)
        assertThat(content[0].taskId).isEqualTo(task3.id)
        assertThat(content[1].taskId).isEqualTo(task2.id)
        assertThat(content[2].taskId).isEqualTo(task1.id)
      }
    }

    @Test
    fun `should support filtering by multiple parameters`() {
      val fromDate = LocalDate.now().minusDays(7)
      val toDate = LocalDate.now()
      val providerCode = "PROVIDER1"

      val matchingAppointment = AppointmentEntity.valid().copy(
        deliusId = 101L,
        providerCode = providerCode,
        date = fromDate.plusDays(1),
      ).persist(ctx)
      saveTask(matchingAppointment)

      val wrongDateAppointment = AppointmentEntity.valid().copy(
        deliusId = 102L,
        providerCode = providerCode,
        date = fromDate.minusDays(1),
      ).persist(ctx)
      saveTask(wrongDateAppointment)

      val wrongProviderAppointment = AppointmentEntity.valid().copy(
        deliusId = 103L,
        providerCode = "OTHER_PROV",
        date = fromDate.plusDays(1),
      ).persist(ctx)
      saveTask(wrongProviderAppointment)

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "theusername",
        appointmentIds = listOf(101L),
        appointments = listOf(NDAppointmentSummary.forAppointment(matchingAppointment)),
        sortString = "name,desc",
        pageSize = 1,
      )

      val pageResponse = webTestClient.get()
        .uri("/admin/appointment-tasks/pending?appointmentFromDate=$fromDate&appointmentToDate=$toDate&appointmentProviderCode=$providerCode&page=0&size=10")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      assertThat(pageResponse.content).hasSize(1)
      assertThat(pageResponse.content[0].appointment.id).isEqualTo(101L)
      assertThat(pageResponse.content[0].appointment.date).isEqualTo(matchingAppointment.date)
    }

    @Test
    fun `should censor names of people who have exclusions or restrictions for the current user`() {
      val appointment = AppointmentEntity.valid().copy(
        deliusId = 101L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "A${String.random(8)}",
        lastName = "A${String.random(8)}",
      ).persist(ctx)
      saveTask(appointment)

      val appointment2 = AppointmentEntity.valid().copy(
        deliusId = 102L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "B${String.random(8)}",
        lastName = "B${String.random(8)}",
      ).persist(ctx)
      saveTask(appointment2)

      val appointment3 = AppointmentEntity.valid().copy(
        deliusId = 103L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "C${String.random(8)}",
        lastName = "C${String.random(8)}",
      ).persist(ctx)
      saveTask(appointment3)

      val appointment4 = AppointmentEntity.valid().copy(
        deliusId = 104L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "D${String.random(8)}",
        lastName = "D${String.random(8)}",
      ).persist(ctx)
      saveTask(appointment4)

      val appointment5 = AppointmentEntity.valid().copy(
        deliusId = 105L,
        providerCode = "PROVIDER1",
        date = LocalDate.now(),
        firstName = "E${String.random(8)}",
        lastName = "E${String.random(8)}",
      ).persist(ctx)
      saveTask(appointment5)

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentsResponse(
        username = "theusername",
        appointments = listOf(
          NDAppointmentSummary.forAppointment(appointment),
          NDAppointmentSummary.forAppointment(appointment2),
          NDAppointmentSummary.forAppointment(appointment3),
          NDAppointmentSummary.forAppointment(appointment4),
          NDAppointmentSummary.forAppointment(appointment5),
        ),
        appointmentIds = listOf(101L, 102L, 103L, 104L, 105L),
        sortString = "name,desc",
        pageSize = 5,
      )

      ProbationAccessControlMockServer.setupGetAccessControlForCrnsResponse(
        "theusername",
        NDCaseAccessItem.excluded(appointment.crn),
        NDCaseAccessItem.restricted(appointment2.crn),
        NDCaseAccessItem.excludedAndRestricted(appointment3.crn),
        NDCaseAccessItem.unrestricted(appointment4.crn),
        NDCaseAccessItem.unrestricted(appointment5.crn),
      )

      val pageResponse = webTestClient.get()
        .uri("/admin/appointment-tasks/pending?sort=appointment.firstName,asc")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<PageResponse<AppointmentTaskSummaryDto>>()

      assertThat(pageResponse.content).hasSize(5)
      // Restricted or excluded, or both
      assertThat(pageResponse.content[0].appointment.id).isEqualTo(101L)
      assertThat(pageResponse.content[0].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      assertThat(pageResponse.content[1].appointment.id).isEqualTo(102L)
      assertThat(pageResponse.content[1].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      assertThat(pageResponse.content[2].appointment.id).isEqualTo(103L)
      assertThat(pageResponse.content[2].offender).isInstanceOf(OffenderDto.OffenderLimitedDto::class.java)
      // Unrestricted
      assertThat(pageResponse.content[3].appointment.id).isEqualTo(104L)
      assertThat(pageResponse.content[3].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      var offender = pageResponse.content[3].offender as OffenderDto.OffenderFullDto
      assertThat(offender.crn).isEqualTo(appointment4.crn)
      assertThat(offender.forename).isNotNull
      assertThat(offender.surname).isNotNull
      assertThat(pageResponse.content[4].appointment.id).isEqualTo(105L)
      assertThat(pageResponse.content[4].offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
      offender = pageResponse.content[4].offender as OffenderDto.OffenderFullDto
      assertThat(offender.crn).isEqualTo(appointment5.crn)
      assertThat(offender.forename).isNotNull
      assertThat(offender.surname).isNotNull
    }

    private fun saveTask(appointment: AppointmentEntity): AppointmentTaskEntity = appointmentTaskEntityRepository.save(
      AppointmentTaskEntity(
        id = UUID.randomUUID(),
        appointment = appointment,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        taskStatus = AppointmentTaskStatus.PENDING,
      ),
    )

    fun NDAppointmentSummary.Companion.forAppointment(appointment: AppointmentEntity) = NDAppointmentSummary.valid(ctx).copy(
      id = appointment.deliusId,
      case = NDCaseSummary.valid().copy(crn = appointment.crn),
      date = appointment.date,
    )
  }

  @Nested
  @DisplayName("PUT /admin/appointment-tasks/{task-id}/complete")
  inner class CompleteTask {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/admin/appointment-tasks/${UUID.randomUUID()}/complete")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/admin/appointment-tasks/${UUID.randomUUID()}/complete")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/admin/appointment-tasks/${UUID.randomUUID()}/complete")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should 404 if task doesn't exist`() {
      webTestClient.put()
        .uri("/admin/appointment-tasks/${UUID.randomUUID()}/complete")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should mark task as completed`() {
      val task = AppointmentTaskEntity.validPending().copy(
        appointment = AppointmentEntity.valid().persist(ctx),
      ).persist(ctx)

      webTestClient.put()
        .uri("/admin/appointment-tasks/${task.id}/complete")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk

      assertThat(appointmentTaskEntityRepository.findByIdOrNull(task.id)!!.taskStatus).isEqualTo(AppointmentTaskStatus.COMPLETE)
    }
  }
}

class FixtureFactory(ctx: ApplicationContext) {
  private val appointmentTaskEntityRepository = ctx.getBean<AppointmentTaskEntityRepository>()
  private val appointmentEntityRepository = ctx.getBean<AppointmentEntityRepository>()
  private val projectTypeEntityRepository = ctx.getBean<ProjectTypeEntityRepository>()

  private val projectTypes = mutableListOf<ProjectTypeEntity>()

  fun projectType(): ProjectTypeEntity = projectTypeEntityRepository.save(ProjectTypeEntity.valid()).also { projectTypes += it }

  fun cleanup() {
    appointmentTaskEntityRepository.deleteAll()
    appointmentEntityRepository.deleteAll()
    projectTypeEntityRepository.deleteAll(projectTypes)
  }
}

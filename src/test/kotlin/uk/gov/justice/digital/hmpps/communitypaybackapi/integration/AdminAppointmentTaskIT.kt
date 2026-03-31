package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.PageResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentTaskSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentTaskType
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import java.time.LocalDate
import java.util.UUID

class AdminAppointmentTaskIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentTaskEntityRepository: AppointmentTaskEntityRepository

  @Autowired
  lateinit var appointmentEntityRepository: AppointmentEntityRepository

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
      val appointment = saveAppointment(date = LocalDate.now(), providerCode = "PROVIDER1", deliusId = 101L)
      saveTask(appointment)

      val matchingAppointmentSummary = NDAppointmentSummary.valid(ctx).copy(
        id = 101L,
        case = NDCaseSummary.valid().copy(crn = appointment.crn),
        date = appointment.date,
      )

      CommunityPaybackAndDeliusMockServer.getAppointments(
        username = "theusername",
        appointments = listOf(matchingAppointmentSummary),
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
    }

    @Test
    fun `should support filtering by multiple parameters`() {
      val fromDate = LocalDate.now().minusDays(7)
      val toDate = LocalDate.now()
      val providerCode = "PROVIDER1"

      val matchingAppointment = saveAppointment(date = fromDate.plusDays(1), providerCode = providerCode, deliusId = 101L)
      saveTask(matchingAppointment)

      val wrongDateAppointment = saveAppointment(date = fromDate.minusDays(1), providerCode = providerCode, deliusId = 102L)
      saveTask(wrongDateAppointment)

      val wrongProviderAppointment = saveAppointment(date = fromDate.plusDays(1), providerCode = "OTHER_PROV", deliusId = 103L)
      saveTask(wrongProviderAppointment)

      val matchingAppointmentSummary = NDAppointmentSummary.valid(ctx).copy(
        id = 101L,
        case = NDCaseSummary.valid().copy(crn = matchingAppointment.crn),
        date = matchingAppointment.date,
      )

      CommunityPaybackAndDeliusMockServer.getAppointments(
        username = "theusername",
        appointmentIds = listOf(101L),
        appointments = listOf(matchingAppointmentSummary),
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

    private fun saveAppointment(date: LocalDate, providerCode: String, deliusId: Long): AppointmentEntity = appointmentEntityRepository.save(
      AppointmentEntity(
        id = UUID.randomUUID(),
        deliusId = deliusId,
        crn = "CRN$deliusId",
        deliusEventNumber = 1,
        createdByCommunityPayback = true,
        date = date,
        providerCode = providerCode,
      ),
    )

    private fun saveTask(appointment: AppointmentEntity): AppointmentTaskEntity = appointmentTaskEntityRepository.save(
      AppointmentTaskEntity(
        id = UUID.randomUUID(),
        appointment = appointment,
        taskType = AppointmentTaskType.ADJUSTMENT_TRAVEL_TIME,
        taskStatus = AppointmentTaskStatus.PENDING,
      ),
    )
  }
}

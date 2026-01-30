package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class AdminAppointmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentEventEntityRepository

  @Autowired
  lateinit var formCacheEntityRepository: FormCacheEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @Nested
  @DisplayName("GET /admin/projects/{projectCode}/appointments/{appointmentId}")
  inner class GetAppointment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/projects/PC01/appointments/101")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/projects/PC01/appointments/101")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/projects/PC01/appointments/101")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.getAppointmentNotFound(
        projectCode = "PC01",
        appointmentId = 101L,
        username = "theusername",
      )

      val response = webTestClient.get()
        .uri("/admin/projects/PC01/appointments/101")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID '101'")
    }

    @Test
    fun `Should return existing appointment with offender info`() {
      val id = 101L
      val projectName = "Community Garden Maintenance"
      val crn = "X434334"

      CommunityPaybackAndDeliusMockServer.getAppointment(
        appointment = NDAppointment.valid().copy(
          id = id,
          project = NDProject.valid().copy(name = projectName, code = "PC01"),
          case = NDCaseSummary.valid().copy(crn = crn),
          outcome = NDContactOutcome.valid(ctx),
          enforcementAction = NDEnforcementAction.valid(ctx),
        ),
        username = "theusername",
      )

      val response = webTestClient.get()
        .uri("/admin/projects/PC01/appointments/101")
        .addAdminUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<AppointmentDto>()

      assertThat(response.id).isEqualTo(id)
      assertThat(response.projectName).isEqualTo(projectName)
    }
  }

  @Nested
  @DisplayName("POST /admin/projects/{projectCode}/appointments/{deliusAppointmentId}/outcome")
  inner class PostAppointmentOutcomeEndpoint {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
      formCacheEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/admin/projects/proj123/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/admin/projects/proj123/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/projects/proj123/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.getAppointmentNotFound(
        projectCode = "proj123",
        appointmentId = 1234L,
        username = "theusername",
      )

      val response = webTestClient.post()
        .uri("/admin/projects/proj123/appointments/1234/outcome")
        .addAdminUiAuthHeader("theusername")
        .bodyValue(
          UpdateAppointmentOutcomeDto.valid(ctx).copy(
            deliusId = 1234L,
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID '1234'")
    }

    @Test
    fun `Should send update upstream, raise domain event and delete corresponding form data`() {
      CommunityPaybackAndDeliusMockServer.getAppointment(
        appointment = NDAppointment.validNoOutcome().copy(
          id = 1234L,
          project = NDProject.valid().copy(code = "proj123"),
          date = LocalDate.now(),
        ),
        username = "theusername",
      )

      CommunityPaybackAndDeliusMockServer.putAppointment(
        projectCode = "proj123",
        appointmentId = 1234L,
      )

      formCacheEntityRepository.save(
        FormCacheEntity(
          formId = "id1",
          formType = "formtype",
          formData = "data",
        ),
      )

      webTestClient.post()
        .uri("/admin/projects/proj123/appointments/1234/outcome")
        .addAdminUiAuthHeader("theusername")
        .bodyValue(
          UpdateAppointmentOutcomeDto.valid(ctx).copy(
            deliusId = 1234L,
            attendanceData = AttendanceDataDto.valid(),
            formKeyToDelete = FormKeyDto(
              id = "id1",
              type = "formtype",
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      CommunityPaybackAndDeliusMockServer.putAppointmentVerify(
        projectCode = "proj123",
        appointmentId = 1234L,
      )

      domainEventAsserter.assertEventCount("community-payback.appointment.updated", 1)

      assertThat(formCacheEntityRepository.count()).isEqualTo(0)
    }
  }
}

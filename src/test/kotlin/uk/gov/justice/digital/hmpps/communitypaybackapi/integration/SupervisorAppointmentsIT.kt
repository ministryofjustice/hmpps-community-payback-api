package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDAppointmentPickUp
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDCaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEvent
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectAndLocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AttendanceDataDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.validNoOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.SchedulingIT.Companion.CRN
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.SchedulingIT.Companion.EVENT_NUMBER
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventAsserter
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class SupervisorAppointmentsIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentEventEntityRepository

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @Nested
  @DisplayName("GET /supervisor/projects/{projectCode}/appointments/{appointmentId}")
  inner class GetAppointment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/supervisor/projects/PC01/appointments/101")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/supervisor/projects/PC01/appointments/101")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/supervisor/projects/PC01/appointments/101")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.setupGetAppointment404Response("PC01", 101L, "theusername")

      val response = webTestClient.get()
        .uri("/supervisor/projects/PC01/appointments/101")
        .addSupervisorUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID 'Project PC01, NDelius ID 101'")
    }

    @Test
    fun `Should return existing appointment with offender info`() {
      val id = 101L
      val projectName = "Community Garden Maintenance"
      val crn = "X434334"

      CommunityPaybackAndDeliusMockServer.setupGetAppointmentResponse(
        appointment = NDAppointment.valid(ctx).copy(
          id = id,
          project = NDProjectAndLocation.valid().copy(name = projectName, code = "PC01"),
          case = NDCaseSummary.valid().copy(crn = crn),
          outcome = NDContactOutcome.valid(ctx),
          enforcementAction = NDEnforcementAction.valid(ctx),
        ),
        username = "theusername",
      )

      val response = webTestClient.get()
        .uri("/supervisor/projects/PC01/appointments/101")
        .addSupervisorUiAuthHeader("theusername")
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<AppointmentDto>()

      assertThat(response.id).isEqualTo(id)
      assertThat(response.projectName).isEqualTo(projectName)
    }
  }

  @Nested
  @DisplayName("PUT /supervisor/projects/{projectCode}/appointments/{deliusAppointmentId}")
  inner class PutAppointment {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/supervisor/projects/PC01/appointments/1234")
        .bodyValue(UpdateAppointmentDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/supervisor/projects/PC01/appointments/1234")
        .bodyValue(UpdateAppointmentDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/supervisor/projects/PC01/appointments/1234")
        .bodyValue(UpdateAppointmentDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.setupPutAppointment404Response(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      val response = webTestClient.put()
        .uri("/supervisor/projects/PC01/appointments/1234")
        .addSupervisorUiAuthHeader()
        .bodyValue(
          UpdateAppointmentDto.valid(ctx).copy(
            deliusId = 1234L,
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID 'Project PC01, NDelius ID 1234'")
    }

    @Test
    fun `Should send update upstream and delete corresponding form data`() {
      CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
        existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
          id = 1234L,
          project = NDProjectAndLocation.valid().copy(code = "PC01"),
          date = LocalDate.now(),
          event = NDEvent.valid().copy(number = EVENT_NUMBER),
          case = NDCaseSummary.valid().copy(crn = CRN),
        ),
        username = "theusername",
        project = NDProject.valid(ctx).copy(code = "PC01"),
      )

      CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      webTestClient.put()
        .uri("/supervisor/projects/PC01/appointments/1234")
        .addSupervisorUiAuthHeader("theusername")
        .bodyValue(
          UpdateAppointmentDto.valid(ctx).copy(
            deliusId = 1234L,
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      CommunityPaybackAndDeliusMockServer.verifyPutAppointmentRequest(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      domainEventAsserter.assertEventCount("community-payback.appointment.updated", 1)
    }
  }

  @Nested
  @DisplayName("POST /supervisor/projects/{projectCode}/appointments/{deliusAppointmentId}/outcome")
  inner class UpdateAppointmentOutcome {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.setupPutAppointment404Response(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      val response = webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/1234/outcome")
        .addSupervisorUiAuthHeader()
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

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID 'Project PC01, NDelius ID 1234'")
    }

    @Test
    fun `Should send update upstream and delete corresponding form data`() {
      CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
        existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
          id = 1234L,
          project = NDProjectAndLocation.valid().copy(code = "PC01"),
          date = LocalDate.now(),
          event = NDEvent.valid().copy(number = EVENT_NUMBER),
          case = NDCaseSummary.valid().copy(crn = CRN),
        ),
        username = "theusername",
        project = NDProject.valid(ctx).copy(code = "PC01"),
      )

      CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/1234/outcome")
        .addSupervisorUiAuthHeader("theusername")
        .bodyValue(
          UpdateAppointmentOutcomeDto.valid(ctx).copy(
            deliusId = 1234L,
            attendanceData = AttendanceDataDto.valid(),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      CommunityPaybackAndDeliusMockServer.verifyPutAppointmentRequest(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      domainEventAsserter.assertEventCount("community-payback.appointment.updated", 1)
    }
  }

  @Nested
  @DisplayName("POST /supervisor/projects/{projectCode}/appointments/bulk")
  inner class BulkUpdate {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/bulk")
        .bodyValue(UpdateAppointmentsDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/bulk")
        .headers(setAuthorisation())
        .bodyValue(UpdateAppointmentsDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/bulk")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .bodyValue(UpdateAppointmentsDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `succeeds and calls upstream endpoint to update the appointment`() {
      val projectAndLocation = NDProjectAndLocation.valid().copy(code = "PC01")
      val project = NDProject.valid(ctx).copy(code = "PC01")
      val pickup = NDAppointmentPickUp.valid()

      CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
        existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
          id = 1234L,
          project = projectAndLocation,
          date = LocalDate.now(),
          event = NDEvent.valid().copy(number = EVENT_NUMBER),
          case = NDCaseSummary.valid().copy(crn = CRN),
          pickUpData = pickup,
        ),
        project = project,
        username = "theusername",
      )

      CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
        projectCode = "PC01",
        appointmentId = 1234L,
      )

      CommunityPaybackAndDeliusMockServer.Aggregates.setupGetDataMocksForUpdateAppointment(
        existingAppointment = NDAppointment.validNoOutcome(ctx).copy(
          id = 5678L,
          project = projectAndLocation,
          date = LocalDate.now(),
          event = NDEvent.valid().copy(number = EVENT_NUMBER),
          case = NDCaseSummary.valid().copy(crn = CRN),
          pickUpData = pickup,
        ),
        project = project,
        username = "theusername",
      )

      CommunityPaybackAndDeliusMockServer.setupPutAppointmentResponse(
        projectCode = "PC01",
        appointmentId = 5678L,
      )

      val result = webTestClient.post()
        .uri("/supervisor/projects/PC01/appointments/bulk")
        .addSupervisorUiAuthHeader("theusername")
        .bodyValue(
          UpdateAppointmentsDto(
            updates = listOf(
              UpdateAppointmentDto.valid(ctx).copy(deliusId = 1234L),
              UpdateAppointmentDto.valid(ctx).copy(deliusId = 5678L),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<UpdateAppointmentsOutcomesResultDto>()

      assertThat(result.results).hasSize(2)
      assertThat(result.results[0].result).isEqualTo(UpdateAppointmentOutcomeResultType.SUCCESS)
      assertThat(result.results[1].result).isEqualTo(UpdateAppointmentOutcomeResultType.SUCCESS)

      CommunityPaybackAndDeliusMockServer.verifyPutAppointmentRequest("PC01", 1234L)
      CommunityPaybackAndDeliusMockServer.verifyPutAppointmentRequest("PC01", 5678L)

      domainEventAsserter.assertEventCount("community-payback.appointment.updated", 2)
    }
  }
}

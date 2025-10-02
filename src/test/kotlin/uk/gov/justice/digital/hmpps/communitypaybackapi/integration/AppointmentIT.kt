package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseName
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummaries
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.dto.OffenderDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.digital.hmpps.communitypaybackapi.project.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntityRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class AppointmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Autowired
  lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @Autowired
  lateinit var projectEntityRepository: ProjectTypeEntityRepository

  @Autowired
  lateinit var domainEventListener: DomainEventListener

  @Nested
  @DisplayName("GET /appointment/{appointmentId}")
  inner class GetAppointment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/appointments/101")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/appointments/101")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/appointments/101")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(101L)

      val response = webTestClient.get()
        .uri("/appointments/101")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID '101'")
    }

    @Test
    fun `Should return existing appointment with offender info`() {
      CommunityPaybackAndDeliusMockServer.projectAppointment(
        ProjectAppointment(
          id = 101L,
          projectName = "Community Garden Maintenance",
          projectCode = "N12345678",
          crn = "CRN1",
          requirementMinutes = 520,
          completedMinutes = 30,
        ),
      )

      CommunityPaybackAndDeliusMockServer.probationCasesSummaries(
        crns = listOf("CRN1"),
        response = CaseSummaries(
          listOf(
            CaseSummary(crn = "CRN1", name = CaseName("Jeff", "Jeffity")),
          ),
        ),
      )

      val response = webTestClient.get()
        .uri("/appointments/101")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<AppointmentDto>()

      assertThat(response.id).isEqualTo(101L)
      assertThat(response.projectName).isEqualTo("Community Garden Maintenance")
      assertThat(response.requirementMinutes).isEqualTo(520)
      assertThat(response.completedMinutes).isEqualTo(30)
      assertThat(response.offender.crn).isEqualTo("CRN1")
      assertThat(response.offender).isInstanceOf(OffenderDto.OffenderFullDto::class.java)
    }
  }

  @Nested
  @DisplayName("PUT /appointments")
  inner class PutAppointmentsEndpoint {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.put()
        .uri("/appointments")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 400 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(1234L)

      val response = webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1234L),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("Validation failure: Appointment not found for ID '1234'")
    }

    @Test
    fun `Should persist single update, raising domain events`() {
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 1L))

      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      val enforcementOutcomeEntity = enforcementActionEntityRepository.findAll().first()
      val projectTypeId = projectEntityRepository.findAll().first().id

      webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1L),
            contactOutcomeId = contactOutcomeEntity.id,
            enforcementActionId = enforcementOutcomeEntity.id,
            projectTypeId = projectTypeId,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      val persistedId = appointmentOutcomeEntityRepository.findAll()[0].id

      val domainEvent = domainEventListener.blockForDomainEventOfType("community-payback.appointment.outcome")
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/domain-event-details/appointment-outcome/$persistedId")
    }

    @Test
    fun `should persist multiple updates, raising domain events`() {
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 1L))
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 2L))
      CommunityPaybackAndDeliusMockServer.projectAppointment(ProjectAppointment.valid().copy(id = 3L))

      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      val enforcementOutcomeEntity = enforcementActionEntityRepository.findAll().first()
      val projectTypeId = projectEntityRepository.findAll().first().id

      webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto.valid(
            ids = longArrayOf(1L, 2L, 3L),
            contactOutcomeId = contactOutcomeEntity.id,
            enforcementActionId = enforcementOutcomeEntity.id,
            projectTypeId = projectTypeId,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      assertThat(
        appointmentOutcomeEntityRepository.findAll()
          .map { it.appointmentDeliusId },
      ).containsExactlyInAnyOrder(1L, 2L, 3L)

      domainEventListener.assertEventCount("community-payback.appointment.outcome", 3)
    }
  }
}

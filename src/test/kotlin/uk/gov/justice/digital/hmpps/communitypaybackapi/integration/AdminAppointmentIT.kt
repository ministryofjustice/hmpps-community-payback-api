package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.CaseSummary
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.Project
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ProjectAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.FormKeyDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.FormCacheEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class AdminAppointmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Autowired
  lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @Autowired
  lateinit var formCacheEntityRepository: FormCacheEntityRepository

  @Autowired
  lateinit var domainEventListener: DomainEventListener

  @Nested
  @DisplayName("GET /admin/appointment/{appointmentId}")
  inner class GetAppointment {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/admin/appointments/101")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/admin/appointments/101")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/admin/appointments/101")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(101L)

      val response = webTestClient.get()
        .uri("/admin/appointments/101")
        .addAdminUiAuthHeader()
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
        ProjectAppointment.valid().copy(
          id = id,
          project = Project.valid().copy(name = projectName),
          case = CaseSummary.valid().copy(crn = crn),
        ),
      )

      val response = webTestClient.get()
        .uri("/admin/appointments/101")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isOk()
        .bodyAsObject<AppointmentDto>()

      assertThat(response.id).isEqualTo(id)
      assertThat(response.projectName).isEqualTo(projectName)
    }
  }

  @Nested
  @DisplayName("PUT /admin/appointments/{deliusAppointmentId}/outcome")
  inner class PutAppointmentOutcomeEndpoint {

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/admin/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/admin/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/admin/appointments/1234/outcome")
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Should return 404 if an appointment can't be found`() {
      CommunityPaybackAndDeliusMockServer.projectAppointmentNotFound(1234L)

      val response = webTestClient.post()
        .uri("/admin/appointments/1234/outcome")
        .addAdminUiAuthHeader()
        .bodyValue(UpdateAppointmentOutcomeDto.valid())
        .exchange()
        .expectStatus()
        .isNotFound()
        .bodyAsObject<ErrorResponse>()

      assertThat(response.userMessage).isEqualTo("No resource found failure: Appointment not found for ID '1234'")
    }

    @Test
    fun `Should persist update, raising domain event, send update and delete form data`() {
      CommunityPaybackAndDeliusMockServer.getAppointment(ProjectAppointment.valid().copy(id = 1234L))
      CommunityPaybackAndDeliusMockServer.putAppointment(1234L)

      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      val enforcementOutcomeEntity = enforcementActionEntityRepository.findAll().first()

      formCacheEntityRepository.save(
        FormCacheEntity(
          formId = "id1",
          formType = "formtype",
          formData = "data",
        ),
      )

      webTestClient.post()
        .uri("/admin/appointments/1234/outcome")
        .addAdminUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomeDto.valid(
            contactOutcomeId = contactOutcomeEntity.id,
            enforcementActionId = enforcementOutcomeEntity.id,
          ).copy(
            formKeyToDelete = FormKeyDto(
              id = "id1",
              type = "formtype",
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk()

      val persistedId = appointmentOutcomeEntityRepository.findAll()[0].id

      val domainEvent = domainEventListener.blockForDomainEventOfType("community-payback.appointment.outcome")
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/domain-event-details/appointment-outcome/$persistedId")

      CommunityPaybackAndDeliusMockServer.putAppointmentVerify(1234L)

      assertThat(formCacheEntityRepository.count()).isEqualTo(0)
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.DomainEventListener
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntityRepository

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
    fun `Should persist single update, raising domain events`() {
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

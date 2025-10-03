package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.dto.AppointmentOutcomeDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.appointment.entity.AppointmentOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.EnforcementActionEntityRepository
import java.util.UUID

class DomainEventDetailsIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Autowired
  lateinit var enforcementActionEntityRepository: EnforcementActionEntityRepository

  @Nested
  @DisplayName("GET /domain-event-details/appointment-outcome/{eventId}")
  inner class GetAppointmentOutcomeDetails {

    val id: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-outcome/$id")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-outcome/$id")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-outcome/$id")
        .addUiAuthHeader()
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 if no entry exists for the ID`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-outcome/${UUID.randomUUID()}")
        .addDomainEventAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `return domain event detail if entry exists`() {
      val entity = appointmentOutcomeEntityRepository.save(
        AppointmentOutcomeEntity.valid(
          contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first(),
          enforcementActionEntity = enforcementActionEntityRepository.findAll().first(),
        ),
      )

      val result = webTestClient.get()
        .uri("/domain-event-details/appointment-outcome/${entity.id}")
        .addDomainEventAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<AppointmentOutcomeDomainEventDetailDto>()

      assertThat(result.id).isEqualTo(entity.id)
    }
  }

  fun <S : WebTestClient.RequestHeadersSpec<S>> S.addDomainEventAuthHeader() = this.headers(
    setAuthorisation(
      username = "DOMAIN_EVENT_CONSUMER",
      roles = listOf(SecurityConfiguration.ROLE_DOMAIN_EVENT_DETAILS),
    ),
  ) as S
}

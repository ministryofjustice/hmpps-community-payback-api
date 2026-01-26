package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.communitypaybackapi.config.SecurityConfiguration
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AppointmentUpdatedDomainEventDetailDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEventEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import java.util.UUID

class DomainEventDetailsIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentEventEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

  @Nested
  @DisplayName("GET /domain-event-details/appointment-updated/{eventId}")
  inner class GetAppointmentUpdatedDetails {

    val id: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      appointmentOutcomeEntityRepository.deleteAll()
    }

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-updated/$id")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-updated/$id")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-updated/$id")
        .addAdminUiAuthHeader()
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return 404 if no entry exists for the ID`() {
      webTestClient.get()
        .uri("/domain-event-details/appointment-updated/${UUID.randomUUID()}")
        .addDomainEventAuthHeader()
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `return domain event detail if entry exists`() {
      val entity = appointmentOutcomeEntityRepository.save(
        AppointmentEventEntity.valid(
          contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first(),
        ),
      )

      val result = webTestClient.get()
        .uri("/domain-event-details/appointment-updated/${entity.id}")
        .addDomainEventAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<AppointmentUpdatedDomainEventDetailDto>()

      assertThat(result.id).isEqualTo(entity.id)
    }

    @Test
    fun `return domain event detail without outcome`() {
      val entity = appointmentOutcomeEntityRepository.save(
        AppointmentEventEntity.valid(
          contactOutcomeEntity = null,
        ),
      )

      val result = webTestClient.get()
        .uri("/domain-event-details/appointment-updated/${entity.id}")
        .addDomainEventAuthHeader()
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<AppointmentUpdatedDomainEventDetailDto>()

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

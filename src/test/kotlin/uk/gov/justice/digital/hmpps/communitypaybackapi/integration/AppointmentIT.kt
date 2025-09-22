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
import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ContactOutcomeEntityRepository

class AppointmentIT : IntegrationTestBase() {

  @Autowired
  lateinit var appointmentOutcomeEntityRepository: AppointmentOutcomeEntityRepository

  @Autowired
  lateinit var contactOutcomeEntityRepository: ContactOutcomeEntityRepository

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
    fun `should persist updates`() {
      val contactOutcomeEntity = contactOutcomeEntityRepository.findAll().first()
      webTestClient.put()
        .uri("/appointments")
        .addUiAuthHeader()
        .bodyValue(UpdateAppointmentOutcomesDto.valid(ids = longArrayOf(1L, 2L, 3L), contactOutcomeId = contactOutcomeEntity.id))
        .exchange()
        .expectStatus()
        .isOk()

      assertThat(
        appointmentOutcomeEntityRepository.findAll()
          .map { it.appointmentDeliusId },
      ).containsExactlyInAnyOrder(1L, 2L, 3L)
    }
  }
}

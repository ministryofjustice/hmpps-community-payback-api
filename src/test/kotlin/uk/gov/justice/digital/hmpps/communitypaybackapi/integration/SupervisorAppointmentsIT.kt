package uk.gov.justice.digital.hmpps.communitypaybackapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.util.bodyAsObject
import uk.gov.justice.digital.hmpps.communitypaybackapi.integration.wiremock.CommunityPaybackAndDeliusMockServer

class SupervisorAppointmentsIT : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /supervisor/appointments/bulk")
  inner class UpdateAppointmentOutcomes {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri("/supervisor/appointments/bulk")
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri("/supervisor/appointments/bulk")
        .headers(setAuthorisation())
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri("/supervisor/appointments/bulk")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .bodyValue(UpdateAppointmentOutcomesDto.valid())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `succeeds and calls upstream endpoint`() {
      CommunityPaybackAndDeliusMockServer.putAppointment(1234L)
      CommunityPaybackAndDeliusMockServer.putAppointment(5678L)

      val result = webTestClient.post()
        .uri("/supervisor/appointments/bulk")
        .addSupervisorUiAuthHeader()
        .bodyValue(
          UpdateAppointmentOutcomesDto(
            updates = listOf(
              UpdateAppointmentOutcomeDto.valid(ctx).copy(deliusId = 1234L),
              UpdateAppointmentOutcomeDto.valid(ctx).copy(deliusId = 5678L),
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

      CommunityPaybackAndDeliusMockServer.putAppointmentVerify(1234L)
      CommunityPaybackAndDeliusMockServer.putAppointmentVerify(5678L)
    }
  }
}

package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.controller.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.toResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto

class BulkUpdateResponseTest {

  @Test
  fun `returns internal server error when every update has a server error`() {
    val result = bulkResult(
      UpdateAppointmentOutcomeResultType.SERVER_ERROR,
      UpdateAppointmentOutcomeResultType.SERVER_ERROR,
    )

    assertThat(result.toResponseEntity().statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `returns bad request when every update has a validation error`() {
    val result = bulkResult(
      UpdateAppointmentOutcomeResultType.VALIDATION_ERROR,
      UpdateAppointmentOutcomeResultType.VALIDATION_ERROR,
    )

    assertThat(result.toResponseEntity().statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `returns OK for mixed results`() {
    val result = bulkResult(
      UpdateAppointmentOutcomeResultType.SUCCESS,
      UpdateAppointmentOutcomeResultType.SERVER_ERROR,
    )

    assertThat(result.toResponseEntity().statusCode).isEqualTo(HttpStatus.OK)
  }

  @Test
  fun `returns OK for no results`() {
    assertThat(bulkResult().toResponseEntity().statusCode).isEqualTo(HttpStatus.OK)
  }

  private fun bulkResult(vararg types: UpdateAppointmentOutcomeResultType) = UpdateAppointmentsOutcomesResultDto(
    results = types.mapIndexed { index, type ->
      UpdateAppointmentOutcomeResultDto(deliusId = index.toLong(), result = type)
    },
  )
}

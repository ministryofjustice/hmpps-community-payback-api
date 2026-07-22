package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.controller.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.toResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeResultType
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentsOutcomesResultDto
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class BulkUpdateResponseTest {

  @Test
  fun `returns internal server error when every update has a server error`() {
    val result = bulkErrorResult(
      UpdateAppointmentOutcomeResultType.SERVER_ERROR to "First server error",
      UpdateAppointmentOutcomeResultType.SERVER_ERROR to "Second server error",
    )

    val response = result.toResponseEntity()

    assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        developerMessage = "First server error; Second server error",
        userMessage = "Unexpected error: First server error; Second server error",
      ),
    )
  }

  @Test
  fun `returns bad request when every update has a validation error`() {
    val result = bulkErrorResult(
      UpdateAppointmentOutcomeResultType.VALIDATION_ERROR to "First validation error",
      UpdateAppointmentOutcomeResultType.VALIDATION_ERROR to "Second validation error",
    )

    val response = result.toResponseEntity()

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.BAD_REQUEST,
        developerMessage = "First validation error; Second validation error",
        userMessage = "Validation failure: First validation error; Second validation error",
      ),
    )
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

  private fun bulkErrorResult(vararg results: Pair<UpdateAppointmentOutcomeResultType, String>) = UpdateAppointmentsOutcomesResultDto(
    results = results.mapIndexed { index, (type, errorMessage) ->
      UpdateAppointmentOutcomeResultDto(deliusId = index.toLong(), result = type, errorMessage = errorMessage)
    },
  )
}

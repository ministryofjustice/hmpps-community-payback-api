package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common

import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ApiResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ApiResponseIssue
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto.valid
import java.util.stream.Stream

class ApiResponseTest {
  @Nested
  inner class SuccessResponse {
    @Test
    fun `should return expected values by default when content is provided`() {
      val content = AdjustmentDto.valid()

      val response = ApiResponse.SuccessResponse(content)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.OK.value())
      assertThat(response.success).isTrue
      assertThat(response.content).isEqualTo(content)
      assertThat(response.errors).isEmpty()
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isTrue
      assertThat(response.hasErrors).isFalse
      assertThat(response.hasWarnings).isFalse
    }

    @Test
    fun `should return expected values by default when content is not provided`() {
      val response = ApiResponse.SuccessResponse<AdjustmentDto>()

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT.value())
      assertThat(response.success).isTrue
      assertThat(response.content).isNull()
      assertThat(response.errors).isEmpty()
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isFalse
      assertThat(response.hasWarnings).isFalse
    }

    @Test
    fun `returns given HTTP status code when provided`() {
      val expectedContent = AdjustmentDto.valid()

      val responseA = ApiResponse.SuccessResponse(expectedContent, status = HttpStatus.CREATED)
      val responseB = ApiResponse.SuccessResponse<AdjustmentDto>(status = HttpStatus.CREATED)

      assertThat(responseA.userMessage).isNull()
      assertThat(responseA.developerMessage).isNull()
      assertThat(responseA.status).isEqualTo(HttpStatus.CREATED.value())
      assertThat(responseA.success).isTrue
      assertThat(responseA.content).isEqualTo(expectedContent)
      assertThat(responseA.errors).isEmpty()
      assertThat(responseA.warnings).isEmpty()
      assertThat(responseA.hasContent).isTrue
      assertThat(responseA.hasErrors).isFalse
      assertThat(responseA.hasWarnings).isFalse

      assertThat(responseB.userMessage).isNull()
      assertThat(responseB.developerMessage).isNull()
      assertThat(responseB.status).isEqualTo(HttpStatus.CREATED.value())
      assertThat(responseB.success).isTrue
      assertThat(responseB.content).isNull()
      assertThat(responseB.errors).isEmpty()
      assertThat(responseB.warnings).isEmpty()
      assertThat(responseB.hasContent).isFalse
      assertThat(responseB.hasErrors).isFalse
      assertThat(responseB.hasWarnings).isFalse
    }

    @Test
    fun `returns warnings when provided`() {
      val expectedContent = AdjustmentDto.valid()
      val expectedWarnings = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE"))

      val responseA = ApiResponse.SuccessResponse(expectedContent, warnings = expectedWarnings)
      val responseB = ApiResponse.SuccessResponse<AdjustmentDto>(warnings = expectedWarnings)

      assertThat(responseA.userMessage).isNull()
      assertThat(responseA.developerMessage).isNull()
      assertThat(responseA.status).isEqualTo(HttpStatus.OK.value())
      assertThat(responseA.success).isTrue
      assertThat(responseA.content).isEqualTo(expectedContent)
      assertThat(responseA.errors).isEmpty()
      assertThat(responseA.warnings).isEqualTo(expectedWarnings)
      assertThat(responseA.hasContent).isTrue
      assertThat(responseA.hasErrors).isFalse
      assertThat(responseA.hasWarnings).isTrue

      assertThat(responseB.userMessage).isNull()
      assertThat(responseB.developerMessage).isNull()
      assertThat(responseB.status).isEqualTo(HttpStatus.NO_CONTENT.value())
      assertThat(responseB.success).isTrue
      assertThat(responseB.content).isNull()
      assertThat(responseB.errors).isEmpty()
      assertThat(responseB.warnings).isEqualTo(expectedWarnings)
      assertThat(responseB.hasContent).isFalse
      assertThat(responseB.hasErrors).isFalse
      assertThat(responseB.hasWarnings).isTrue
    }
  }

  @Nested
  inner class ErrorResponse {
    @Test
    fun `should return expected values by default when errors are provided`() {
      val expectedErrors = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE"))

      val response = ApiResponse.ErrorResponse<AdjustmentDto>(expectedErrors)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(response.success).isFalse
      assertThat(response.content).isNull()
      assertThat(response.errors).isEqualTo(expectedErrors)
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isTrue
      assertThat(response.hasWarnings).isFalse
    }

    @Test
    fun `returns given HTTP status code when provided`() {
      val expectedErrors = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE"))

      val response = ApiResponse.ErrorResponse<AdjustmentDto>(expectedErrors, status = HttpStatus.CONFLICT)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.CONFLICT.value())
      assertThat(response.success).isFalse
      assertThat(response.content).isNull()
      assertThat(response.errors).isEqualTo(expectedErrors)
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isTrue
      assertThat(response.hasWarnings).isFalse
    }

    @Test
    fun `returns warnings when provided`() {
      val expectedErrors = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE"))
      val expectedWarnings = listOf(ApiResponseIssue("$.deliusId", "ID_IN_OLD_FORMAT"))

      val response = ApiResponse.ErrorResponse<AdjustmentDto>(expectedErrors, warnings = expectedWarnings)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(response.success).isFalse
      assertThat(response.content).isNull()
      assertThat(response.errors).isEqualTo(expectedErrors)
      assertThat(response.warnings).isEqualTo(expectedWarnings)
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isTrue
      assertThat(response.hasWarnings).isTrue
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.communitypaybackapi.unit.common.ApiResponseTest#throwables")
    fun `returns expected values when constructed using a throwable`(
      throwable: Throwable,
      expectedStatus: HttpStatus,
      expectedFieldName: String,
      expectedErrorCode: String,
      expectedUserMessage: String,
      expectedDeveloperMessage: String,
    ) {
      val expectedErrors = listOf(ApiResponseIssue(expectedFieldName, expectedErrorCode, mapOf("message" to throwable.message)))

      val response = ApiResponse.ErrorResponse<AdjustmentDto>(throwable)

      assertThat(response.userMessage).isEqualTo(expectedUserMessage)
      assertThat(response.developerMessage).isEqualTo(expectedDeveloperMessage)
      assertThat(response.status).isEqualTo(expectedStatus.value())
      assertThat(response.success).isFalse
      assertThat(response.content).isNull()
      assertThat(response.errors).isEqualTo(expectedErrors)
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isTrue
      assertThat(response.hasWarnings).isFalse
    }
  }

  @Nested
  inner class FromValidationResult {
    @Test
    fun `returns a success when the validation result is successful`() {
      val validationWarnings = listOf(ValidationResultItem("$.reasonCode", "UNKNOWN_REASON_CODE", emptyMap()))
      val validationResult = ValidationResult(
        errors = emptyList(),
        warnings = validationWarnings,
      )

      val expectedWarnings = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE", emptyMap()))
      val expectedContent = AdjustmentDto.valid()
      val response = ApiResponse.fromValidationResult(validationResult, expectedContent, HttpStatus.CREATED, HttpStatus.INTERNAL_SERVER_ERROR)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.CREATED.value())
      assertThat(response.success).isTrue
      assertThat(response.content).isEqualTo(expectedContent)
      assertThat(response.errors).isEmpty()
      assertThat(response.warnings).isEqualTo(expectedWarnings)
      assertThat(response.hasContent).isTrue
      assertThat(response.hasErrors).isFalse
      assertThat(response.hasWarnings).isTrue
    }

    @Test
    fun `returns an error when the validation result is unsuccessful`() {
      val validationErrors = listOf(ValidationResultItem("$.reasonCode", "UNKNOWN_REASON_CODE", emptyMap()))
      val validationResult = ValidationResult(
        errors = validationErrors,
        warnings = emptyList(),
      )

      val expectedErrors = listOf(ApiResponseIssue("$.reasonCode", "UNKNOWN_REASON_CODE", emptyMap()))
      val expectedContent = AdjustmentDto.valid()
      val response = ApiResponse.fromValidationResult(validationResult, expectedContent, HttpStatus.CREATED, HttpStatus.INTERNAL_SERVER_ERROR)

      assertThat(response.userMessage).isNull()
      assertThat(response.developerMessage).isNull()
      assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
      assertThat(response.success).isFalse
      assertThat(response.content).isNull()
      assertThat(response.errors).isEqualTo(expectedErrors)
      assertThat(response.warnings).isEmpty()
      assertThat(response.hasContent).isFalse
      assertThat(response.hasErrors).isTrue
      assertThat(response.hasWarnings).isFalse
    }

    @Test
    fun `returns OK as default HTTP status when validation result is successful and content is provided`() {
      val validationResult = ValidationResult.success()

      val response = ApiResponse.fromValidationResult(validationResult, AdjustmentDto.valid())
      assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `returns No Content as default HTTP status when validation result is successful and no content is provided`() {
      val validationResult = ValidationResult.success()

      val response = ApiResponse.fromValidationResult<AdjustmentDto>(validationResult)
      assertThat(response.status).isEqualTo(HttpStatus.NO_CONTENT.value())
    }

    @Test
    fun `returns Bad Request as default HTTP status when validation result is unsuccessful`() {
      val validationErrors = listOf(ValidationResultItem("$.reasonCode", "UNKNOWN_REASON_CODE", emptyMap()))
      val validationResult = ValidationResult(
        errors = validationErrors,
        warnings = emptyList(),
      )

      val response = ApiResponse.fromValidationResult<AdjustmentDto>(validationResult)
      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }
  }

  companion object {
    @JvmStatic
    fun throwables(): Stream<Arguments> = listOf(
      Arguments.of(
        ValidationException("some validation issue"),
        HttpStatus.BAD_REQUEST,
        "",
        "GENERIC_VALIDATION_ERROR",
        "Validation failure: some validation issue",
        "some validation issue",
      ),
      Arguments.of(
        MissingServletRequestParameterException("param1", "Int"),
        HttpStatus.BAD_REQUEST,
        "param1",
        "MISSING_REQUEST_PARAMETER",
        "Validation failure: Required request parameter 'param1' for method parameter type Int is not present",
        "Required request parameter 'param1' for method parameter type Int is not present",
      ),
      Arguments.of(
        MissingServletRequestPartException("param1"),
        HttpStatus.BAD_REQUEST,
        "param1",
        "MISSING_REQUEST_PART",
        "Validation failure: Required part 'param1' is not present.",
        "Required part 'param1' is not present.",
      ),
      Arguments.of(
        BadRequestException("some issue"),
        HttpStatus.BAD_REQUEST,
        "",
        "BAD_REQUEST",
        "Validation failure: some issue",
        "some issue",
      ),
      Arguments.of(
        mockk<MethodArgumentNotValidException> {
          every { message } returns "Some detail about the invalid arg"
          every { parameter } returns mockk {
            every { parameterName } returns "param1"
          }
        },
        HttpStatus.BAD_REQUEST,
        "param1",
        "INVALID_REQUEST_PARAMETER",
        "Validation failure: Some detail about the invalid arg",
        "Some detail about the invalid arg",
      ),
      Arguments.of(
        MethodArgumentTypeMismatchException(
          null,
          Int::class.java,
          "param1",
          mockk {
            every { parameterName } returns "param1"
          },
          null,
        ),
        HttpStatus.BAD_REQUEST,
        "param1",
        "INVALID_REQUEST_PARAMETER",
        "Validation failure: Method parameter 'param1': Failed to convert value of type 'null' to required type 'int'",
        "Method parameter 'param1': Failed to convert value of type 'null' to required type 'int'",
      ),
      Arguments.of(
        HttpMessageNotReadableException("something's wrong with the message", mockk()),
        HttpStatus.BAD_REQUEST,
        "",
        "BAD_REQUEST",
        "Validation failure: something's wrong with the message",
        "something's wrong with the message",
      ),
      Arguments.of(
        NoResourceFoundException(HttpMethod.GET, "/some/path/to/something", "/some/path/to/something"),
        HttpStatus.NOT_FOUND,
        "",
        "NOT_FOUND",
        "No resource found failure: No static resource /some/path/to/something for request '/some/path/to/something'.",
        "No static resource /some/path/to/something for request '/some/path/to/something'.",
      ),
      Arguments.of(
        NotFoundException("The thing could not be found"),
        HttpStatus.NOT_FOUND,
        "",
        "NOT_FOUND",
        "No resource found failure: The thing could not be found",
        "The thing could not be found",
      ),
      Arguments.of(
        ConflictException("Something could not be updated"),
        HttpStatus.CONFLICT,
        "",
        "CONFLICT",
        "Something could not be updated",
        "Something could not be updated",
      ),
      Arguments.of(
        AccessDeniedException("Someone tried to access something they shouldn't"),
        HttpStatus.FORBIDDEN,
        "",
        "ACCESS_DENIED",
        "Forbidden: Someone tried to access something they shouldn't",
        "Someone tried to access something they shouldn't",
      ),
      Arguments.of(
        object : RuntimeException("Some other miscellaneous exception") {},
        HttpStatus.INTERNAL_SERVER_ERROR,
        "",
        "UNKNOWN_ERROR",
        "Unexpected error: Some other miscellaneous exception",
        "Some other miscellaneous exception",
      ),
    ).stream()
  }
}

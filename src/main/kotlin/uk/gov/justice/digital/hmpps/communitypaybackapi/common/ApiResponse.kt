package uk.gov.justice.digital.hmpps.communitypaybackapi.common

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResult
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.validation.ValidationResultItem
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException

typealias ApiResponseEntity<T> = ResponseEntity<ApiResponse<T>>

typealias ErrorResponse = ApiResponse.ErrorResponse<Unit>

sealed interface ApiResponse<T> {
  @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
  @get:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
  val userMessage: String?

  @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
  @get:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
  val developerMessage: String?

  val status: Int
  val success: Boolean
  val content: T?
  val errors: List<ApiResponseIssue>
  val warnings: List<ApiResponseIssue>

  val hasContent: Boolean
    get() = content != null
  val hasErrors: Boolean
    get() = errors.isNotEmpty()
  val hasWarnings: Boolean
    get() = warnings.isNotEmpty()

  fun toResponseEntity(): ApiResponseEntity<T> = ResponseEntity
    .status(this.status)
    .body(this)

  data class SuccessResponse<T>(
    override val status: Int,
    override val content: T?,
    override val warnings: List<ApiResponseIssue> = emptyList(),
  ) : ApiResponse<T> {
    constructor(content: T, status: HttpStatus = HttpStatus.OK, warnings: List<ApiResponseIssue> = emptyList()) : this(status.value(), content, warnings)
    constructor(status: HttpStatus = HttpStatus.NO_CONTENT, warnings: List<ApiResponseIssue> = emptyList()) : this(status.value(), null, warnings)

    @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
    @get:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
    override val userMessage: String? = null

    @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
    @get:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
    override val developerMessage: String? = null

    override val success = true
    override val errors = emptyList<ApiResponseIssue>()
  }

  data class ErrorResponse<T>(
    override val status: Int,
    override val errors: List<ApiResponseIssue>,
    override val warnings: List<ApiResponseIssue> = emptyList(),
    @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
    @param:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
    override val userMessage: String? = null,
    @Deprecated("Included for backwards compatibility. Use 'errors' instead.")
    @param:Schema(description = "Included for backwards compatibility. Use 'errors' instead.", deprecated = true)
    override val developerMessage: String? = null,
  ) : ApiResponse<T> {
    constructor(errors: List<ApiResponseIssue>, status: HttpStatus = HttpStatus.BAD_REQUEST, warnings: List<ApiResponseIssue> = emptyList()) : this(status.value(), errors, warnings)
    constructor(throwable: Throwable) : this(
      throwable.status.value(),
      listOf(ApiResponseIssue(throwable.fieldName ?: "", throwable.errorCode, mapOf("message" to throwable.message))),
      emptyList(),
      throwable.userMessage,
      throwable.message,
    )

    override val success = false
    override val content: T? = null
  }

  companion object {
    private val Throwable.status: HttpStatus
      get() = when (this) {
        is ValidationException,
        is MissingServletRequestParameterException,
        is MissingServletRequestPartException,
        is BadRequestException,
        is MethodArgumentNotValidException,
        is MethodArgumentTypeMismatchException,
        is HttpMessageNotReadableException,
        -> HttpStatus.BAD_REQUEST
        is NoResourceFoundException, is NotFoundException -> HttpStatus.NOT_FOUND
        is ConflictException -> HttpStatus.CONFLICT
        is AccessDeniedException -> HttpStatus.FORBIDDEN
        else -> HttpStatus.INTERNAL_SERVER_ERROR
      }

    private val Throwable.userMessage: String
      get() = when (this) {
        is ValidationException,
        is MissingServletRequestParameterException,
        is MissingServletRequestPartException,
        is BadRequestException,
        is MethodArgumentNotValidException,
        is MethodArgumentTypeMismatchException,
        is HttpMessageNotReadableException,
        -> "Validation failure: ${this.message}"
        is NoResourceFoundException, is NotFoundException -> "No resource found failure: ${this.message}"
        is ConflictException -> this.message
        is AccessDeniedException -> "Forbidden: ${this.message}"
        else -> "Unexpected error: ${this.message}"
      }

    private val Throwable.fieldName: String?
      get() = when (this) {
        is MissingServletRequestParameterException -> this.parameterName
        is MissingServletRequestPartException -> this.requestPartName
        is MethodArgumentNotValidException -> this.parameter.parameterName
        is MethodArgumentTypeMismatchException -> this.parameter.parameterName
        else -> null
      }

    private val Throwable.errorCode: String
      get() = when (this) {
        is ValidationException -> "GENERIC_VALIDATION_ERROR"
        is MissingServletRequestParameterException -> "MISSING_REQUEST_PARAMETER"
        is MissingServletRequestPartException -> "MISSING_REQUEST_PART"
        is BadRequestException, is HttpMessageNotReadableException -> "BAD_REQUEST"
        is MethodArgumentNotValidException, is MethodArgumentTypeMismatchException -> "INVALID_REQUEST_PARAMETER"

        is NoResourceFoundException, is NotFoundException -> "NOT_FOUND"

        is ConflictException -> "CONFLICT"

        is AccessDeniedException -> "ACCESS_DENIED"

        else -> "UNKNOWN_ERROR"
      }

    fun <T> fromValidationResult(result: ValidationResult, content: T? = null, successStatus: HttpStatus? = null, errorStatus: HttpStatus? = null): ApiResponse<T> {
      val warnings = result.warnings.map(::ApiResponseIssue)
      return if (result.isSuccess) {
        val defaultStatus = content?.let { HttpStatus.OK } ?: HttpStatus.NO_CONTENT
        SuccessResponse((successStatus ?: defaultStatus).value(), content, warnings)
      } else {
        val errors = result.errors.map(::ApiResponseIssue)
        ErrorResponse(errors, errorStatus ?: HttpStatus.BAD_REQUEST, warnings)
      }
    }
  }
}

data class ApiResponseIssue(val field: String, val code: String, val data: Map<String, Any?> = emptyMap()) {
  constructor(validationResultItem: ValidationResultItem) : this(validationResultItem.field, validationResultItem.code, validationResultItem.data)
}

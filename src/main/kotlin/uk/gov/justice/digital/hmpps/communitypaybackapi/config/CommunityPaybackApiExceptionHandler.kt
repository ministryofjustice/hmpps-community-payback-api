package uk.gov.justice.digital.hmpps.communitypaybackapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ApiResponseEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.common.ErrorResponse
import uk.gov.justice.digital.hmpps.communitypaybackapi.controller.internal.NotFoundException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.BadRequestException
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.NDeliusRollbackService
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.internal.SentryService

@RestControllerAdvice
class CommunityPaybackApiExceptionHandler(
  val sentryService: SentryService,
  val rollbackService: NDeliusRollbackService,
) {

  @ExceptionHandler(
    ValidationException::class,
    MissingServletRequestParameterException::class,
    MissingServletRequestPartException::class,
    BadRequestException::class,
    MethodArgumentNotValidException::class,
    MethodArgumentTypeMismatchException::class,
    HttpMessageNotReadableException::class,
  )
  fun validationError(e: Exception): ApiResponseEntity<Unit> = ErrorResponse(e)
    .toResponseEntity()
    .also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(
    NoResourceFoundException::class,
    NotFoundException::class,
  )
  fun handleNoResourceFoundException(e: Exception): ApiResponseEntity<Unit> = ErrorResponse(e)
    .toResponseEntity()
    .also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(
    ConflictException::class,
  )
  fun handleConflictException(e: Exception): ApiResponseEntity<Unit> = ErrorResponse(e)
    .toResponseEntity()
    .also { log.info("Conflict exception: {}", e.message) }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ApiResponseEntity<Unit> = ErrorResponse(e)
    .toResponseEntity()
    .also { log.debug("Forbidden (403) returned: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ApiResponseEntity<Unit> = ErrorResponse(e)
    .toResponseEntity()
    .also { rollbackService.publishEventsForRollback() }
    .also { sentryService.captureException(e) }
    .also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

fun buildNoResourceFoundResponse(message: String?) = ErrorResponse(NotFoundException(message ?: ""))
  .toResponseEntity()

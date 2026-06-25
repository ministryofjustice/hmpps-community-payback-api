package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionDraftResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun CourseCompletionDraftResolutionDto.Companion.valid() = CourseCompletionDraftResolutionDto(
  crn = String.random(7),
)

fun CourseCompletionDraftResolutionDto.Companion.validEmpty() = CourseCompletionDraftResolutionDto(
  crn = null,
)

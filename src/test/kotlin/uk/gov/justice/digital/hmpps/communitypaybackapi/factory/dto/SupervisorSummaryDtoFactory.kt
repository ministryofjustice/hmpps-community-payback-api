package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.GradeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.NameDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.SupervisorSummaryDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import kotlin.String

fun SupervisorSummaryDto.Companion.valid() = SupervisorSummaryDto(
  code = String.Companion.random(5),
  name = NameDto.valid(),
  fullName = String.random(30),
  grade = GradeDto(
    code = String.random(5),
    description = String.random(50),
  ),
  unallocated = Boolean.random(),
)

fun NameDto.Companion.valid() = NameDto(
  forename = String.random(50),
  surname = String.random(50),
  middleNames = listOf(String.random(50), String.random(50)),
)

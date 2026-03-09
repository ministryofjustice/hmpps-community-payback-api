package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionCreditTimeDetailsDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionResolutionTypeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import kotlin.random.Random

fun CourseCompletionResolutionDto.Companion.valid() = CourseCompletionResolutionDto(
  crn = String.random(1).uppercase() + Random.nextInt(0, 99999),
  type = CourseCompletionResolutionTypeDto.entries.random(),
  creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(),
)

fun CourseCompletionResolutionDto.Companion.valid(ctx: ApplicationContext) = CourseCompletionResolutionDto.valid().copy(
  creditTimeDetails = CourseCompletionCreditTimeDetailsDto.valid(ctx),
)

fun CourseCompletionCreditTimeDetailsDto.Companion.valid() = CourseCompletionCreditTimeDetailsDto(
  deliusEventNumber = Long.random(50),
  appointmentIdToUpdate = Long.random(),
  date = randomLocalDate(),
  minutesToCredit = Long.random(0, 181),
  contactOutcomeCode = String.random(20),
  projectCode = String.random(20),
  notes = String.random(50),
  alertActive = Boolean.random(),
  sensitive = Boolean.random(),
)

fun CourseCompletionCreditTimeDetailsDto.Companion.valid(ctx: ApplicationContext) = CourseCompletionCreditTimeDetailsDto.valid().copy(
  contactOutcomeCode = ctx.getBean<ContactOutcomeEntityRepository>().findAll().minByOrNull { it.name }!!.code,
)

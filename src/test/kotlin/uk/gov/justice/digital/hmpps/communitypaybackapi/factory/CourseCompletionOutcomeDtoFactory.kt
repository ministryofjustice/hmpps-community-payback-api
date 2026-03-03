package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.CourseCompletionOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import kotlin.random.Random

fun CourseCompletionOutcomeDto.Companion.valid() = CourseCompletionOutcomeDto(
  crn = String.random(1).uppercase() + Random.nextInt(0, 99999),
  deliusEventNumber = Long.random(50),
  appointmentIdToUpdate = Long.random(),
  minutesToCredit = Long.random(0, 181),
  contactOutcomeCode = String.random(20),
  projectCode = String.random(20),
)

fun CourseCompletionOutcomeDto.Companion.valid(ctx: ApplicationContext) = CourseCompletionOutcomeDto.valid().copy(
  contactOutcomeCode = ctx.getBean<ContactOutcomeEntityRepository>().findAll().minByOrNull { it.name }!!.code,
)

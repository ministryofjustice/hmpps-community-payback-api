package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.dto

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository

fun UpdateAppointmentOutcomesDto.Companion.valid() = UpdateAppointmentOutcomesDto(
  updates = listOf(UpdateAppointmentOutcomeDto.valid()),
)

fun UpdateAppointmentOutcomeDto.Companion.valid(ctx: ApplicationContext) = UpdateAppointmentOutcomeDto.valid(
  contactOutcomeCode = ctx.getBean<ContactOutcomeEntityRepository>().findAll().first().code,
)

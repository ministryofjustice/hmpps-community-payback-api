package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomeDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.dto.UpdateAppointmentOutcomesDto
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository

fun UpdateAppointmentOutcomesDto.Companion.valid() = UpdateAppointmentOutcomesDto(
  updates = listOf(UpdateAppointmentOutcomeDto.valid()),
)

fun UpdateAppointmentOutcomeDto.Companion.valid(ctx: ApplicationContext) = UpdateAppointmentOutcomeDto.valid(
  contactOutcomeId = ctx.getBean(ContactOutcomeEntityRepository::class.java).findAll().first().id,
  enforcementActionId = ctx.getBean(EnforcementActionEntityRepository::class.java).findAll().first().id,
)

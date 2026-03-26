package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionEventResolutionEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EteCourseCompletionResolution
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomOffsetDateTime
import java.util.UUID
import kotlin.String

fun EteCourseCompletionEventResolutionEntity.Companion.valid() = EteCourseCompletionEventResolutionEntity(
  id = UUID.randomUUID(),
  eteCourseCompletionEvent = EteCourseCompletionEventEntity.valid(),
  resolution = EteCourseCompletionResolution.entries.random(),
  createdAt = randomOffsetDateTime(),
  createdByUsername = String.random(5),
  crn = String.random(5),
  deliusEventNumber = Int.random(),
  deliusAppointmentId = Long.random(),
  deliusAppointmentCreated = Boolean.random(),
  projectCode = String.random(5),
  minutesCredited = Long.random(600),
  contactOutcome = ContactOutcomeEntity.valid(),
)

fun EteCourseCompletionEventResolutionEntity.Companion.valid(ctx: ApplicationContext) = EteCourseCompletionEventResolutionEntity.valid().copy(
  contactOutcome = ctx.getBean<ContactOutcomeEntityRepository>().findAll().minByOrNull { it.name }!!,
)

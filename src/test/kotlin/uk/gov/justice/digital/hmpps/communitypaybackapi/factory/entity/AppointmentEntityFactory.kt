package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.AppointmentEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate
import java.util.UUID

fun AppointmentEntity.Companion.valid() = AppointmentEntity(
  id = UUID.randomUUID(),
  deliusId = Long.random(),
  crn = String.random(5),
  deliusEventNumber = Int.random().toLong(),
  createdByCommunityPayback = Boolean.random(),
  date = randomLocalDate(),
)

fun AppointmentEntity.persist(ctx: ApplicationContext) = ctx.getBean<AppointmentEntityRepository>().save(this)

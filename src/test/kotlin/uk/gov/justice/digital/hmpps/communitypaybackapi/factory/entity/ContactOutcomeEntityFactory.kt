package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.entity

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntity
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import java.util.UUID

fun ContactOutcomeEntity.Companion.valid() = ContactOutcomeEntity(
  id = UUID.randomUUID(),
  code = String.random(length = 3),
  name = String.random(length = 20),
  enforceable = false,
  attended = false,
  groups = emptyList(),
)

fun ContactOutcomeEntity.persist(ctx: ApplicationContext): ContactOutcomeEntity = ctx.getBean<ContactOutcomeEntityRepository>().save(this)

package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.ContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun ContactOutcome.Companion.valid() = ContactOutcome(
  code = String.random(5),
  description = String.random(20),
)

fun ContactOutcome.Companion.valid(ctx: ApplicationContext) = ContactOutcome.valid()
  .copy(
    code = ctx.getBean(ContactOutcomeEntityRepository::class.java).findAll().first().code,
  )

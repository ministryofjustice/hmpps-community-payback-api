package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDContactOutcome
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ContactOutcomeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDContactOutcome.Companion.valid() = NDContactOutcome(
  code = String.random(5),
  description = String.random(20),
)

fun NDContactOutcome.Companion.valid(ctx: ApplicationContext) = NDContactOutcome.valid()
  .copy(
    code = ctx.getBean(ContactOutcomeEntityRepository::class.java).findAll().first().code,
  )

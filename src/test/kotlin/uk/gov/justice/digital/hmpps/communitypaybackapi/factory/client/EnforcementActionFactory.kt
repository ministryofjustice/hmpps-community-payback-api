package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.EnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun EnforcementAction.Companion.valid() = EnforcementAction(
  code = String.random(5),
  description = String.random(20),
  respondBy = randomLocalDate(),
)

fun EnforcementAction.Companion.valid(ctx: ApplicationContext) = EnforcementAction.valid()
  .copy(
    code = ctx.getBean(EnforcementActionEntityRepository::class.java).findAll().first().code,
  )

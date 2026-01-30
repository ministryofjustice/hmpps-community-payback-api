package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDEnforcementAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.EnforcementActionEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.randomLocalDate

fun NDEnforcementAction.Companion.valid() = NDEnforcementAction(
  code = String.random(5),
  description = String.random(20),
  respondBy = randomLocalDate(),
)

fun NDEnforcementAction.Companion.valid(ctx: ApplicationContext) = NDEnforcementAction.valid()
  .copy(
    code = ctx.getBean(EnforcementActionEntityRepository::class.java).findAll().first().code,
  )

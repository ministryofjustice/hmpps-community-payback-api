package uk.gov.justice.digital.hmpps.communitypaybackapi.factory.client

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import uk.gov.justice.digital.hmpps.communitypaybackapi.client.NDProjectType
import uk.gov.justice.digital.hmpps.communitypaybackapi.entity.ProjectTypeEntityRepository
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random

fun NDProjectType.Companion.valid() = NDProjectType(
  code = String.random(),
  name = String.random(),
)

fun NDProjectType.Companion.valid(ctx: ApplicationContext): NDProjectType {
  val projectType = ctx.getBean<ProjectTypeEntityRepository>().findAll().first()

  return NDProjectType(
    code = projectType.code,
    name = projectType.name,
  )
}

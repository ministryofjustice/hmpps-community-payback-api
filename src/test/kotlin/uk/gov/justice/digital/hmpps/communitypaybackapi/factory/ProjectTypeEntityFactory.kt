package uk.gov.justice.digital.hmpps.communitypaybackapi.factory

import uk.gov.justice.digital.hmpps.communitypaybackapi.reference.entity.ProjectTypeEntity
import java.util.UUID

fun ProjectTypeEntity.Companion.valid() = ProjectTypeEntity(
  id = UUID.randomUUID(),
  code = String.random(length = 3),
  name = String.random(length = 20),
)
